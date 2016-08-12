package hck.moraledex.lambda

import java.text.DecimalFormat
import java.time.LocalDate
import java.time.temporal.ChronoField

import hck.moraledex.commons.{MD5, StandaloneWSAPI}
import play.api.libs.ws._
import awscala._
import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.config.ConfigFactory
import dynamodbv2._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.libs.json._


class FeedbackAnalyzer  {

  val config = ConfigFactory.load()

  implicit val region = Region.US_EAST_1

  implicit val dynamoDB = DynamoDB(config.getString("aws.accessId"), config.getString("aws.accessKey"))

  val table: Table = dynamoDB.table("Feedback").get

  //POST
  def updateSentimentDb(context: Context) = {

    val result = pullDataFromWorkday()

    val json = Json.parse(result)

    for(entry <- (json \ "Report_Entry").as[JsArray].value.map(_.as[JsObject])) yield  {

      for{
        subOrg <- (entry \ "supOrg").toOption.map(_.as[String])
        subOrgId <- (entry \ "SupOrg_WID").toOption.map(_.as[String])
        workerId <- (entry \ "Worker_WID").toOption.map(_.as[String])
        location <- (entry \ "location").toOption.map(_.as[String])
      } yield {

        val managementLevel = (entry \ "managementLevel").toOption.map(_.as[String])
        val manChainLevel03 = (entry \ "manChainLevel03").toOption.map(_.as[String])
        val continent = (entry \ "locationHierarchyLevel2").toOption.map(_.as[String]).map{ text =>
          text.substring(text.indexOf(" ") + 1)
        }

        for{
          feedback <- (entry \ "Feedback_Received").as[JsArray].value.map(_.as[JsObject])
          timestamp <- (feedback \ "timestamp").toOption.map(_.as[String]).map { text =>
            val formatter = new DecimalFormat("00")
            val now = LocalDate.now()
            val m = formatter.format((Math.random()* now.get(ChronoField.MONTH_OF_YEAR)).toInt + 1)
            val d = formatter.format((Math.random()* 28).toInt + 1)
            val t = s"2016-${m}-${d}T18:26:53.265-07:00"
            t
          }
          description <- (feedback \ "Description").toOption.map(_.as[String]).map{ text =>
            text.substring(text.lastIndexOf(":") + 2)
          }
        } yield {

          val id = MD5.hash(subOrg + description) + subOrg.length + description.length

          if(table.getItem(id, location).isEmpty) {
            val analizerUrl = config.getString("google.api.endpoint") + "?key=" + config.getString("google.api.key")
            Await.result(StandaloneWSAPI(analizerUrl) { request =>
              val data = JsObject(Seq("document" -> JsObject(Seq("content" -> JsString(description), "type" -> JsString("PLAIN_TEXT")))))
              request.post(data.toString)
            } map { response =>
              val sentiment = Json.parse(response)
              val polarity = (sentiment \ "documentSentiment" \ "polarity").toOption.map(_.as[Double]).getOrElse(0.0)
              val magnitude = (sentiment \ "documentSentiment" \ "magnitude").toOption.map(_.as[Double]).getOrElse(0.0)
              val score = new DecimalFormat("0.00").format(polarity * magnitude).toDouble
              pushDataToWorkday(id, subOrgId, workerId, description, score, timestamp)
              table.put(id, location, "subOrg" -> subOrg, "subOrgId" -> subOrgId, "workerId" -> workerId,
                "managementLevel" -> managementLevel, "manChainLevel03" -> manChainLevel03,
                "location" -> location, "continent" -> continent, "description" -> description, "timestamp" -> timestamp,
                "polarity" -> polarity, "magnitude" -> magnitude, "sentiment" -> score)

            }, 10 seconds)
          }

        }
      }
    }//end for

  }

  def pullDataFromWorkday(): String = {
    val endpoint = config.getString("suv.api.pull.endpoint")
    val user = config.getString("suv.api.user")
    val password = config.getString("suv.api.password")
    Await.result(StandaloneWSAPI(endpoint){ request =>
      request.withAuth(user, password, WSAuthScheme.BASIC).get()
    }, 100 seconds)
  }

  def pushDataToWorkday(id: String, subOrgId: String, workerId: String, comment: String, sentiment: Double, timestamp: String) = {
    val endpoint = config.getString("suv.api.push.endpoint")
    println(">>>>>>>>>>>>>>>>>>>" + Await.result(StandaloneWSAPI(endpoint){ request =>
      val data = JsObject(Seq(
        "supervisoryOrganization" -> JsObject(Seq("id" -> JsString(subOrgId))),
        "from" -> JsArray(Array(JsObject(Seq("id" -> JsString(workerId))))),
        "comment" -> JsString(comment),
        "sentiment" -> JsNumber(sentiment),
        "reference" -> JsString(id),
        "date" -> JsString(timestamp)
      ))
      println("============" + data)
      val user = config.getString("suv.api.user")
      val password = config.getString("suv.api.password")
      request.withAuth(user, password, WSAuthScheme.BASIC).post(data)
    }, 5 seconds))
  }


  //GET
  def getSentiments(context: Context): String = {
    val items: Seq[Item] = table.scan(Seq("location" -> cond.isNotNull))
    println(items.size)
    val locationSentiments = items.toList.map{ item =>
      val location = item.attributes.find(_.name =="location").head.value.s.get
      val sentiment = item.attributes.find(_.name =="sentiment").head.value.n.map(_.toDouble).getOrElse(0.0)
      (location, sentiment)
    }
    Json.toJson(locationSentiments.groupBy(_._1).map{
      case (k, v) => (k, v.map(_._2))
    }).toString
  }

}

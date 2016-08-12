package hck.moraledex.commons

import play.api.libs.ws.{WSAPI, WSClient, WSRequest, WSResponse}
import play.api.libs.ws.ning.NingWSClientConfig
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by hussachai.puripunpin on 7/30/16.
  */
object StandaloneWSAPI extends WSAPI {

  import play.api.libs.ws.ning.{NingWSClient, NingAsyncHttpClientConfigBuilder}

  lazy val configuration = new NingAsyncHttpClientConfigBuilder(NingWSClientConfig()).build()

  override val client: WSClient = new NingWSClient(configuration)

  override def url(url: String): WSRequest = client.url(url)

  def apply(url: String)(execute: WSRequest => Future[WSResponse]): Future[String] = {
    val request = client.url(url)
    execute(request).map{ response =>
      response.body
    }
  }

  def close() = client.close()

  sys.addShutdownHook{
    close()
  }

}



package hck.moraledex.commons

/**
  * Created by hussachai.puripunpin on 7/31/16.
  */
object Models {

  case class Feedback(subOrg: String,
                      managementLevel: Option[String],
                      manChainLevel03: Option[String],
                      location: String,
                      continent: Option[String],
                      feedback: String,
                      timestamp: String,
                      polarity: Double,
                      magnitude: Double,
                      score: Double)

}

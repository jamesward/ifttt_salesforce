package controllers.ifttt.v1

import play.api.http.MimeTypes
import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object User extends Controller {

  private def createWebhook(authToken: String, instanceUrl: String, name: String, sobject: String, events: Seq[String], webhookUrl: String) = {

    val webhookCreatorUrl = "https://salesforce-webhook-creator.herokuapp.com/webhooks"

    val headers = Seq("X-SESSION-ID" -> authToken, "X-INSTANCE-URL" -> instanceUrl)

    val json = Json.obj(
      "name" -> name,
      "sobject" -> sobject,
      "events" -> events,
      "url" -> webhookUrl,
      "rollbackOnError" -> false
    )

    WS.url(webhookCreatorUrl).withHeaders(headers: _*).post(json)
  }

  def info = Action.async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(auth) =>
        val userinfoFuture = WS.
          url("https://login.salesforce.com/services/oauth2/userinfo").
          withHeaders(AUTHORIZATION -> auth).
          get()

        userinfoFuture.map { response =>
          response.status match {
            case OK =>

              val authToken = auth.stripPrefix("Bearer ")

              val instanceUrl = (response.json \ "profile").as[String].stripSuffix("/" + (response.json \ "user_id").as[String])

              createWebhook(authToken, instanceUrl, "IFTTTOpportunity", "Opportunity", Seq("after update"), routes.Webhooks.opportunityWasWon().absoluteURL())

              val jsonResult = response.json.transform {
                val reads = {
                  (__ \ 'data \ 'id).json.copyFrom((__ \ 'user_id).json.pick) and
                    (__ \ 'data \ 'name).json.copyFrom((__ \ 'name).json.pick) and
                    (__ \ 'data \ 'url).json.copyFrom((__ \ 'profile).json.pick)
                }
                reads.reduce
              }

              jsonResult match {
                case JsSuccess(json, _) =>
                  Ok(json)
                case JsError(error) =>
                  InternalServerError("JSON was malformed: " + error.toString)
              }
            case FORBIDDEN =>
              val json = Json.obj(
                "errors" -> Json.arr(
                  Json.obj(
                    "status" -> response.body,
                    "message" -> ("Authentication failed: " + response.body)
                  )
                )
              )
              Unauthorized(json)
            case _ =>
              Status(response.status)(response.body)
          }

        }
      case None =>
        Future.successful(Unauthorized("Request did not contain an Authorization header"))
    }
  }

}
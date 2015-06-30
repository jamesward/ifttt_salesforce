package controllers.ifttt.v1

import play.api.libs.json.{JsValue, JsString, JsObject, Json}
import play.api.mvc.{Action, Controller}
import utils.ForceUtils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object Actions extends Controller {

  def postOnChatter = Action.async(parse.json) { request =>

    request.headers.get(AUTHORIZATION).fold(Future.successful(Unauthorized(""))) { auth =>

      val maybeMessage = (request.body \ "actionFields" \ "message").asOpt[String]

      maybeMessage.fold(Future.successful(BadRequest(error("MISSING_REQUIRED_FIELD", "Message field was missing")))) { message =>

        ForceUtils.chatterPost(auth, message).map {
          case (response, Some(instanceUrl)) if response.status == CREATED =>

            val id = (response.json \ "id").as[String]

            val json = Json.obj(
              "data" -> Json.arr(
                Json.obj(
                  "id" -> id,
                  "url" -> (instanceUrl + id)
                )
              )
            )

            Ok(json)

          case (response, None) if response.status == FORBIDDEN =>
            Unauthorized(error("Unauthorized", response.body))

          case (response, _) =>
            Status(response.status)(response.body)
        }
      }
    }
  }

  def insertARecord() = Action.async(parse.json) { request =>

    request.headers.get(AUTHORIZATION).fold(Future.successful(Unauthorized(""))) { auth =>

      val maybeSobject = (request.body \ "actionFields" \ "sobject").asOpt[String]

      def maybeNameValue(num: Int): Option[(String, JsValue)] = {
        (request.body \ "actionFields" \ s"field_name_$num").asOpt[String].map { fieldName1 =>
          fieldName1 -> request.body \ "actionFields" \ s"field_value_$num"
        }
      }

      val jsonToInsert = JsObject((1 to 5).flatMap(maybeNameValue))

      maybeSobject.fold(Future.successful(BadRequest(error("MISSING_REQUIRED_FIELD", "The sobject actionField is required")))) { sobject =>

        ForceUtils.insert(auth, sobject, jsonToInsert).map {
          case response if response.status == CREATED =>

            val id = (response.json \ "id").as[String]

            val json = Json.obj(
              "data" -> Json.arr(
                Json.obj(
                  "id" -> id
                )
              )
            )

            Ok(json)

          case response if response.status == FORBIDDEN =>
            Unauthorized(error("Unauthorized", response.body))

          case response =>
            Status(response.status)(response.body)
        }
      }
    }
  }

  def insertARecordFieldsSObjectOptions() = ForceUtils.sobjectOptions()

  private def error(status: String, message: String): JsObject = {
    Json.obj(
      "errors" -> Json.arr(
        Json.obj(
          "status" -> status,
          "message" -> message
        )
      )
    )
  }


}

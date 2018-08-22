package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsFalse, JsNumber, JsString, JsTrue, JsValue, JsonFormat}


/**
  * Defines the structure of a  response from a REST api call.  It has metadata  as well as the optional
  * response payload .
  *
  * @param meta
  * @param response
  * @tparam T
  */
case class ApiResponse[T](meta: ApiResponseMetaData, response: Option[T] = None)

/**
  * Detailed information(Meta Data) about the response that will contain status code and any error information if there was an error
  *
  * @param statusCode
  * @param error
  */
case class ApiResponseMetaData(statusCode: Int, error: Option[ErrorMessage] = None)

/**
  * Created by AYON SANYAL on 27-05-2018.
  */
trait ApiResponseProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val errorMessageFormat = jsonFormat2(ErrorMessage.apply)
  implicit val metaFormat = jsonFormat2(ApiResponseMetaData)

  implicit def serviceapiResponseFormat[T: JsonFormat] = jsonFormat2(ApiResponse.apply[T])

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(data: Any) = data match {
      case number: Int => JsNumber(number)
      case string: String => JsString(string)
      case bool: Boolean if bool == true => JsTrue
      case bool: Boolean if bool == false => JsFalse
    }

    def read(value: JsValue) = value match {
      case JsNumber(number) => number.intValue()
      case JsString(string) => string
      case JsTrue => true
      case JsFalse => false
    }
  }

}


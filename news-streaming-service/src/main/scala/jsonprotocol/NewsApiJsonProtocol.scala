package jsonprotocol

import common.{ApiResponseProtocol}
import spray.json._

import domain.{HeadLines, NewsSource, NewsTitle, NotFoundForSource}


/**
  * Format for the response which will be produced by
  * the service.
  * Created by AYON SANYAL on 28-05-2018.
  */
trait NewsApiJsonProtocol extends ApiResponseProtocol {


  implicit val printer = PrettyPrinter
  implicit val source = jsonFormat2(NewsSource)
  implicit val titles = jsonFormat1(NewsTitle)

  implicit val headLines = jsonFormat2(HeadLines)


  implicit val notFoundHeadLines = jsonFormat1(NotFoundForSource)


}

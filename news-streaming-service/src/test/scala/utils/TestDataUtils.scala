package utils

import common.{ErrorMessage, Failure, FailureType}
import domain.{HeadLines, NewsSource, NewsTitle, NotFoundHeadLines}

import scala.collection.immutable.TreeSet

/**
  * Created by AYON SANYAL on 28-05-2018.
  */
object TestDataUtils {

  val dummyResponse = Set(HeadLines(NewsSource("cnn", "CNN"), Set(NewsTitle("France beats Italy in international friendly"),
    NewsTitle("Real Madrid beats LiverPool in UEFA final "))),
    HeadLines(NewsSource("bbc", "BBC"), Set(NewsTitle("AFG beaten BAN"),
      NewsTitle("Brazil beaten Croatia "))))

  implicit val orderingForFailureSources = Ordering[String].on[NotFoundHeadLines](_.sourceName)
  val headLinesNotFoundMock = TreeSet(NotFoundHeadLines("ayon"), NotFoundHeadLines("sanyal"))
  val nonReliableSource = ErrorMessage("wrong source name", Some(s"News not found for any source.  please try with valid source"))
  val failureResponse = Failure(FailureType.Validation, nonReliableSource)


}

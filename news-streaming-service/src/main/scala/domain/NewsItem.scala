package domain


/** A domain which holds retrieval of newsItem i.e if found or not not found.
  * In short it is a wrapper over NewsFeed.
  * Created by AYON SANYAL on 02-06-2018.
  */
sealed trait NewsItem {
  def sourceName: String
}

case class NotFoundHeadLines(sourceName: String) extends NewsItem

case class NewsItemFound(headLines: HeadLines) extends NewsItem {
  def sourceName = headLines.source.name
}

case object InvalidSource extends NewsItem {
  def sourceName = ""
}

case class NotFoundForSource(sourceName: String)

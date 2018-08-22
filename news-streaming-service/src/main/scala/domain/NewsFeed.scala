package domain

/**
  * Created by AYON SANYAL on 27-05-2018.
  */

case class NewsSource(id: String, name: String)

case class NewsTitle(title: String)


case class HeadLines(source: NewsSource, titles: Set[NewsTitle]) {

}

case class Preference(sources: String)
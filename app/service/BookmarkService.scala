package service

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.IteratorHasAsScala

class BookmarkService {
  private val bookmarks = new ConcurrentLinkedQueue[String]()

  def bookmark(movie: String): Unit = {
    bookmarks.add(movie)
  }

  def listBookmarks(): List[String] = bookmarks.iterator().asScala.toList
}

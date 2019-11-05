package crawler

import scala.annotation.tailrec
import scala.tools.jline.console.ConsoleReader
import scala.util.{Failure, Success}

object CrawlerScript {

  def main(args: Array[String]): Unit = {
    crawl(true)
  }


  /**
    * method to retrieve url from the user, setup the file store,
    * start the crawl and present the returned json to the user.
    *
    */
  @tailrec
  def crawl(running: Boolean): Unit = {

    val crawler: WebCrawler = new WebCrawler()

    if (running) {

      val consoleReader: ConsoleReader = new tools.jline.console.ConsoleReader()

      val url: String = consoleReader.readLine("Please enter the url you wish to crawl:")

      println("Crawling........")

      crawler.urlCheck(url) match {
        case Success(_) => println(crawler.crawl(url))
        case Failure(e) => println(s"url: $url not valid ---$e")
      }

      println("Do you wish to continue? (Y or N)")
      val exit: Boolean = scala.io.StdIn.readBoolean()

      crawl(running = exit)
    }
  }

}

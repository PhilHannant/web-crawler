package crawler

import java.io.{File, FileOutputStream, PrintWriter}

import adt.DomainAssetsDerives._
import adt.{DomainAssets, UrlAssets}
import cats.data.EitherT
import io.circe.Json
import io.circe.syntax._
import io.lemonlabs.uri.Url
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import scala.annotation.tailrec
import scala.collection.{immutable, mutable}
import scala.io.Source
import scala.util.{Failure, Success, Try}


class WebCrawler {

  /**
    * class val to set location of file store
    */
  val linksToVisit: File = new File(System.getProperty("user.dir") + "/links.txt")

  val userAgent: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36"

  /**
    * Method that initiates ths crawl and returns json containing the domain assets
    */
  def crawl(url: String): Json = {
    val assets: Seq[(String, Set[String])] = fetchAssets(url)
    val domainAssets: DomainAssets = makeDomains(assets, url)
    domainAssets.asJson
  }

  /**
    *
    * Iterative solution to visit every url inside an a[href] and then
    * extract the media present on that url.
    *
    * Returning a sequence containing assets mapped tho their url
    */
  def fetchAssets(url: String): Seq[(String, Set[String])] = {

    val domain: String = extractHost(url)

    var visitedUrls: Set[String] = Set.empty[String]

    var mappedAssets: Map[String, Set[String]] = Map()

    val urls: mutable.Queue[String] = mutable.Queue[String]()

    urls += url

    while (urls.nonEmpty) {
      val urlToVisit = urls.dequeue()
      if (!visitedUrls.contains(urlToVisit) && compareDomains(domain, urlToVisit)) {
        visitedUrls += urlToVisit

        println(s"$urlToVisit")
        val document: Either[Unit, Document] = fetchDocument(urlToVisit)

        document match {
          case Left(()) => println(s"broken link for $document")
          case Right(doc) =>
            val links: Elements = doc.select("a[href]")
            val media: Elements = doc.select("[src], link[href]")

            media.forEach { m =>
              mappedAssets = mappedAssets + (urlToVisit -> (mappedAssets.getOrElse(urlToVisit, Set.empty[String]) ++ extractAssets(m)))
            }

            links.forEach { l =>
              val nextUrl = l.absUrl("href")
              if (compareDomains(domain, urlToVisit) && !visitedUrls.contains(nextUrl)) urls += nextUrl
            }
        }
      }
    }

    mappedAssets.toSeq

  }


  /**
    * Converts sequence returned from fetchAssets to the DomainAssets case class
    * containing all mapped assets
    */
  def makeDomains(allAssets: Seq[(String, Set[String])], originalUrl: String): DomainAssets = {

    @tailrec
    def makeDomainsHelper(assetMapping: Seq[(String, Set[String])], domains: Seq[UrlAssets]): Seq[UrlAssets] = {
      assetMapping match {
        case (url, assets) :: Nil => domains ++ Seq(UrlAssets(url, assets))
        case Nil => domains ++ Seq(UrlAssets("", Set()))
        case (url, assets) :: xs => makeDomainsHelper(xs, domains ++ Seq(UrlAssets(url, assets)))
      }
    }

    DomainAssets(makeDomainsHelper(allAssets, Seq()))
  }

  /**
    * Extracts stylesheet, images and script assets for element provided
    */
  def extractAssets(element: Element): Set[String] = {
    assets(element.select("link[href]"), "href") ++ assets(element.select("[src]"), "src") ++ assets(element.getElementsByTag("script"), "src")
  }

  /**
    * Extracts assets relating to provided key from elements provided adding them to a mutable set
    */
  def assets(elemments: Elements, key: String): Set[String] = {
    var assetSet: Set[String] = Set.empty[String]

    elemments.forEach { elem =>
      assetSet += elem.attr(key)
    }
    assetSet
  }

  /**
    * Try's the url provided to ensure it correct and can be used to crawl
    */
  def urlCheck(url: String): Try[Document] = {
    Try(Jsoup.connect(url)
      .userAgent(userAgent)
      .timeout(5000)
      .ignoreContentType(true)
      .get)
  }

  /**
    * Extracts host/domain from original url provided
    *
    * https://www.example.com -> example.com
    */
  def extractHost(url: String): String =
    Url.parse(url).apexDomain.getOrElse(url)


  /**
    * Compares provided url with original url to see if they are on the same domain
    */
  def compareDomains(domain: String, url: String): Boolean = {
    val urlDomain: Try[Option[String]] = Try(Url.parse(url).apexDomain)
    urlDomain match {
      case Success(d) => d.getOrElse("") == domain
      case Failure(_) => false
    }
  }

  /**
    * Try's to obtain the document from the url provided, if successful
    * the fetch document is returned else a unit value is
    */
  def fetchDocument(url: String): Either[Unit, Document] = {
    val document = Try(Jsoup.connect(url)
      .userAgent(userAgent)
      .timeout(5000)
      .ignoreContentType(true)
      .get)
    document match {
      case Success(doc) => Right(doc)
      case Failure(_) => Left()
    }
  }

}

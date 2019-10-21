package crawler

import java.io.{File, FileOutputStream, PrintWriter}

import adt.DomainAssetsDerives._
import adt.{DomainAssets, UrlAssets}
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
    val assets: Seq[(String, mutable.Set[String])] = fetchAssets(url)
    val domainAssets: DomainAssets = makeDomains(assets, url)
    domainAssets.asJson
  }

  /**
    *
    * Recursive solution to visit every url inside an a[href] and then
    * extract the media present on that url.
    *
    * Returning a sequence containing assets mapped tho their url
    */
  def fetchAssets(url: String): Seq[(String, mutable.Set[String])] = {

    val domain: String = extractHost(url)

    var mappedAssets: Map[String, mutable.Set[String]] = Map()

    def fetchLinksHelper(url: String, urls: mutable.Set[String]): Unit = {

      if (readFile().contains(url) || !compareDomains(domain, url)) return

      writeFile(url)

      val document: Either[Unit, Document] = fetchDocument(url)

      document match {
        case Left(()) => ()
        case Right(doc) =>
          val links: Elements = doc.select("a[href]")
          val media: Elements = doc.select("[src], link[href]")

          media.forEach { m =>
            mappedAssets = mappedAssets + (url -> (mappedAssets.getOrElse(url, mutable.Set()) ++ extractAssets(m)))
          }

          links.forEach { l =>
              fetchLinksHelper (l.absUrl ("href"), urls)
          }
      }
    }

    fetchLinksHelper(url, mutable.Set())

    mappedAssets.toSeq

  }

  /**
    * Converts sequence returned from fetchAssets to the DomainAssets case class
    * containing all mapped assets
    */
  def makeDomains(allAssets: Seq[(String, mutable.Set[String])], originalUrl: String): DomainAssets = {

    @tailrec
    def makeDomainsHelper(assetMapping: Seq[(String, mutable.Set[String])], domains: Seq[UrlAssets]): Seq[UrlAssets] = {
      assetMapping match {
        case (url, assets) :: Nil => {
          val finalDomains = domains ++ Seq(UrlAssets(url, assets.toSet))
          finalDomains
        }
        case Nil => {
          println(s"No assets found for $originalUrl")
          domains ++ Seq(UrlAssets("", Set()))
        }
        case (url, assets) :: xs => makeDomainsHelper(xs, domains ++ Seq(UrlAssets(url, assets.toSet)))
      }
    }

    DomainAssets(makeDomainsHelper(allAssets, Seq()))
  }

  /**
    * Extracts stylesheet, images and script assets for element provided
    */
  def extractAssets(element: Element): mutable.Set[String] = {
    assets(element.select("link[href]"), "href") ++ assets(element.select("[src]"), "src") ++ assets(element.getElementsByTag("script"), "src")
  }

  /**
    * Extracts assets relating to provided key from elements provided adding them to a mutable set
    */
  def assets(elemments: Elements, key: String): mutable.Set[String] = {
    val assetSet: mutable.Set[String] = mutable.Set()

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
  def compareDomains(domain: String, url: String): Boolean =
    Url.parse(url).apexDomain.getOrElse("") == domain


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

  /**
    * Method to create file store on start up and delete it on exit
    * always runs a delete first to ensure a clean file is there for each crawl
    */
  def setUpLinksFile(): Unit = {
    linksToVisit.delete()
    linksToVisit.createNewFile()
    linksToVisit.deleteOnExit()
  }

  /**
    * Writes urls that have been visited to file store
    */
  private def writeFile(urls: String): Unit = {
    val writer = new PrintWriter(new FileOutputStream(linksToVisit, true))
    writer.write(urls + "\n")
    writer.close()
  }

  /**
    * Try's to read the file store if successful returns a set containing urls visited
    * else returns an empty set
    */
  private def readFile(): immutable.Set[String] = {
    val linkFile = Try(Source.fromFile(linksToVisit))
      linkFile match {
        case Success(file) => file.getLines().toSet
        case Failure(_) => immutable.Set()
      }
  }

}

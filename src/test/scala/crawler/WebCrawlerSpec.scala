package crawler

import java.io.File

import adt.{DomainAssets, UrlAssets}
import io.circe.Json
import io.circe.parser._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.collection.mutable
import scala.io.Source
import scala.util.{Failure, Success, Try}

class WebCrawlerSpec extends Specification with Mockito {


  "web crawler" should {

    "crawl should take a url and should return json containing the domain assets" in new Context {

      val successfulJson: Json = parse("""{
                           |  "domain" : [
                           |    {
                           |      "url" : "https://www.exampleSite.com",
                           |      "assets" : [
                           |        "https://www.exampleSite.com/style.css",
                           |        "https://www.exampleSite.com/script.js"
                           |      ]
                           |    },
                           |    {
                           |      "url" : "https://www.exampleSite.com/page1",
                           |      "assets" : [
                           |        "https://www.exampleSite.com/page1/script.js",
                           |        "https://www.exampleSite.com/page1/style.css",
                           |        "https://www.exampleSite.com/page1/exampleimage.jpg"
                           |      ]
                           |    }
                           |  ]
                           |}""".stripMargin).getOrElse(Json.fromString("failed"))



      val result: Json = crawler.crawl(testUrl)
      result must beEqualTo(successfulJson)

    }

    "crawl should return empty json fields if there are no assets" in new Context {

      val failedJson: Json = parse("""{
                                       |  "domain" : [
                                       |    {
                                       |      "url" : "",
                                       |      "assets" : [
                                       |      ]
                                       |    }
                                       |  ]
                                       |}""".stripMargin).getOrElse(Json.fromString("failed"))


      val result: Json = crawler.crawl("www.noassetsurl.co.uk")
      result must beEqualTo(failedJson)


    }

    "fetch links" in new Context {

      val expected: Seq[(String, mutable.Set[String])] = Seq(("https://www.exampleSite.com", assetSetHomePage), ("https://www.exampleSite.com/page1", assetSetPage1))

      val result: Seq[(String, mutable.Set[String])] = crawler.fetchAssets("https://www.exampleSite.com")

      result must beEqualTo(expected)

    }

    "makeDomain should return all DomainsAssets" in new Context {

      val assets: Seq[(String, mutable.Set[String])] = Seq(("https://www.exampleSite.com", assetSetHomePage), ("https://www.exampleSite.com/page1", assetSetPage1))

      val expected = DomainAssets(Seq(homePage, page1))

      val result: DomainAssets = crawler.makeDomains(assets, testUrl)

      result must beEqualTo(expected)

    }

    "extract href assets" in new Context {

      val elements: Elements = exampleSite1Html.select("link[href]")

      val result: mutable.Set[String] = crawler.assets(elements, "href")

      val expected: mutable.Set[String] = mutable.Set("https://www.exampleSite.com/style.css")

      result must beEqualTo(expected)
    }

    "extract src assets" in new Context {

      val elements: Elements = testDocument.select("[src]")

      val result: mutable.Set[String] = crawler.assets(elements, "src")

      val expected: mutable.Set[String] = mutable.Set("../images/left1.gif", "../images/nov.gif", "../images/right1.gif")

      result must beEqualTo(expected)
    }

    "fetchDocument should return a Right when successful" in new Context {

      val result: Either[Unit, Document] = crawler.fetchDocument(testUrl)

      result must beRight(exampleSite1Html)

    }

    "fetchDocument should return a Left when unable to connect to url" in new Context {

      val result: Either[Unit, Document] = crawler.fetchDocument("www.failure.")

      result must beLeft(())

    }

    "extractHost should return the host" in new Context {

      val expected = "exampleSite.com"

      crawler.extractHost(testUrl) must beEqualTo(expected)

    }

    "compareDomains should check if a url is on the same domain as the original" in new Context {

      val domain: String = "exampleSite.com"

      val urlOnDomain: String = "https://test.exampleSite.com"
      val urlNotOnDomain: String = "test.example.co.uk"

      crawler.compareDomains(domain, urlOnDomain) must beTrue
      crawler.compareDomains(domain, urlNotOnDomain) must beFalse
    }

    "urlCheck if the starting url is valid" in new Context {

      crawler.urlCheck(testUrl) must beSuccessfulTry
      crawler.urlCheck("notValid.com") must beFailedTry

    }


  }


  trait Context extends Scope {

    val testUrl: String = "https://www.exampleSite.com"

    val assetSetHomePage: mutable.Set[String] = mutable.Set("https://www.exampleSite.com/style.css", "https://www.exampleSite.com/script.js")
    val assetSetPage1: mutable.Set[String] = mutable.Set("https://www.exampleSite.com/page1/style.css", "https://www.exampleSite.com/page1/script.js", "https://www.exampleSite.com/page1/exampleimage.jpg")

    val exampleSite1: File = new File(System.getProperty("user.dir") + "/ExampleSite-1.html")
    val exampleSite2: File = new File(System.getProperty("user.dir") + "/ExampleSite-2.html")

    val exampleSite1Html: Document = Jsoup.parse(Source.fromFile(new File(exampleSite1.toURI)).mkString)
    val exampleSite2Html: Document = Jsoup.parse(Source.fromFile(new File(exampleSite2.toURI)).mkString)

    val crawler: WebCrawler = new WebCrawler {

      override def fetchDocument(url: String): Either[Unit, Document] = url match {
        case "https://www.exampleSite.com" => Right(exampleSite1Html)
        case "https://www.exampleSite.com/page1" => Right(exampleSite2Html)
        case _ => Left(())
      }

      override def urlCheck(url: String): Try[Document] = url match {
        case "https://www.exampleSite.com"  => Success(exampleSite1Html)
        case _ => Failure(new Exception("IllegalArgumentException"))
      }
    }

    val homePage: UrlAssets = UrlAssets("https://www.exampleSite.com", assetSetHomePage.toSet)
    val page1: UrlAssets = UrlAssets("https://www.exampleSite.com/page1", assetSetPage1.toSet)

    crawler.setUpLinksFile()

    val testHtml = "<a href=\"../blog.html\" onmouseover=\"return setStatus('click to continue...');\" onmouseout=\"return setStatus('');\"><img src=\"../images/nov.gif\" width=\"18\" height=\"6\" border=\"0\" alt=\"November\"></a>\n<a href=\"blog021125.html\" onmouseover=\"roll2('left1','_over');return setStatus('previous days blog');\" onmouseout=\"roll2('left1');return setStatus('');\"><img src=\"../images/left1.gif\" width=\"6\" height=\"5\" border=\"0\" alt=\"previous day's blog\" name=\"left1\"></a>\n<a href=\"blog021127.html\" onmouseover=\"roll2('right1','_over');return setStatus('next days blog');\" onmouseout=\"roll2('right1');return setStatus('');\"><img src=\"../images/right1.gif\" width=\"6\" height=\"5\" border=\"0\" alt=\"next day's blog\" name=\"right1\"></a>"

    val testDocument: Document = Jsoup.parse(testHtml)

  }

}
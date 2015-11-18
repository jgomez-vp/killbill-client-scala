package org.killbill.billing.client.actor

import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.TestKit
import akka.util.Timeout
import org.killbill.billing.client.model.Credit
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import spray.http._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source

/**
  * Created by jgomez on 16/11/2015.
  */
class CreditActorSpec extends TestKit(ActorSystem()) with SpecificationLike with Mockito {

  import CreditActor._

  implicit val timeout = Timeout(Duration(5, TimeUnit.SECONDS))

  // Test Get Credit by Id method
  def getCreditTest() = {
    val mockResponse = mock[HttpResponse]
    val mockStatus = mock[StatusCode]
    mockResponse.status returns mockStatus
    mockStatus.isSuccess returns true
    val getCreditStream: InputStream = getClass.getResourceAsStream("/getCreditResponse.json")
    val getCreditJsonContent = Source.fromInputStream(getCreditStream, "UTF-8").getLines.mkString
    val getCreditBodyResponse = HttpEntity(MediaTypes.`application/json`, getCreditJsonContent.getBytes())
    mockResponse.entity returns getCreditBodyResponse

    val creditActor = system.actorOf(Props(new CreditActor("AnyUrl", mock[List[HttpHeader]]) {
      override def sendAndReceive = {
        (req:HttpRequest) => Future.apply(mockResponse)
      }
    }), name = "GetCreditActor")

    "GetCredit should" >> {
      "return Credit object" in {
        val fut: Future[Any] = ask(creditActor, GetCredit(UUID.randomUUID())).mapTo[Any]
        val getCreditResponse = Await.result(fut, timeout.duration)
        val expected = Credit(Option(60), Option("b17298d2-37fc-4701-8b9d-92ab1d15f01c"), Option("b17298d2-37fc-4701-8b9d-92ab1d15f01c"),
          Option.apply("2015-11-15"), Option("b17298d2-37fc-4701-8b9d-92ab1d15f01c"))
        getCreditResponse mustEqual expected
      }
    }
  }

  // Test Create Credit method
  def createCreditTest() = {
    val mockResponse = mock[HttpResponse]
    val mockStatus = mock[StatusCode]
    mockResponse.status returns mockStatus
    mockStatus.isSuccess returns true

    val createCreditBodyResponse = HttpEntity(MediaTypes.`application/json`, "201 Created")
    mockResponse.entity returns createCreditBodyResponse

    val creditActor = system.actorOf(Props(new CreditActor("AnyUrl", mock[List[HttpHeader]]) {
      override def sendAndReceive = {
        (req:HttpRequest) => Future.apply(mockResponse)
      }
    }), name = "CreateCreditActor")

    "CreateCredit should" >> {
      "return String response" in {
        val credit: Credit = Credit.apply(Option.apply(300), None, None, None, Option.apply("570f2248-d85b-4235-975b-23607b2b37db"))
        val fut: Future[String] = ask(creditActor, CreateCredit(credit)).mapTo[String]
        val createCreditResponse = Await.result(fut, timeout.duration.+(Duration(10, TimeUnit.SECONDS)))
        val expected = "201 Created"
        createCreditResponse mustEqual expected
      }
    }
  }

  getCreditTest()
  createCreditTest()
}
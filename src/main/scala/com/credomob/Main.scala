package com.credomob

import akka.actor.ActorSystem

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, ValidationRejection}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.credomob.AkkaHttpServer.{Donuts, HttpJsonSupport}
import com.credomob.routes.{DonutRoutes, ServerVersion}
import com.credomob.utils.VendorPointConfig.http
import com.typesafe.scalalogging.LazyLogging
import spray.json.DefaultJsonProtocol

import scala.io.StdIn
import scala.util.{Failure, Success}

object AkkaHttpServer extends App with LazyLogging{

  implicit val system = ActorSystem("vendingPoint")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  //Route Set Up =======================================================================
  //val serverUpRoute: Route = get { complete("Akka is Up") }

  implicit val globalRejectionHandler = RejectionHandler.newBuilder()
    .handle{ case ValidationRejection(msg,route) =>
        complete(StatusCodes.InternalServerError,s"The operation is not supported, error = $msg ,route =$route")
    }
    .handleNotFound{
      complete(StatusCodes.NotFound,"The path is not supported.")
    }
    .result()

  implicit val globalExceptionHandler = ExceptionHandler{
    case exception: RuntimeException => complete(s"A runtime exception occurred with, msg=${exception.getMessage}")
  }

  val serverVersionRoute = new ServerVersion().route()
  val serverJsonVersionRoute = new ServerVersion().routeAsJson()
  val serverJsonAsJson = new ServerVersion().routeAsJsonEncoding()

  val donutRoutes = new DonutRoutes().route()
  val routes:Route = donutRoutes ~ serverVersionRoute ~ serverJsonVersionRoute ~ serverJsonAsJson


  //Models Set Up ======================================================================
  final case class AkkaHttpRestServer(app:String, version:String)
  final case class Donut(name:String,price:Double)
  final case class Donuts(donuts: Seq[Donut])
  final case class Ingredient(donutName:String, priceLevel:Double)

  trait HttpJsonSupport extends SprayJsonSupport with DefaultJsonProtocol{
    import spray.json._
    implicit val printer = PrettyPrinter

    implicit val akkaHttpRestServerFormat = jsonFormat2(AkkaHttpRestServer)
    implicit val donutFormat = jsonFormat2(Donut)
    implicit val donutsJsonFormat = jsonFormat1(Donuts)
  }


  //Server Set Up ======================================================================
  val host = http.host
  val port = http.port
  val httpServerFuture: Future[ServerBinding] = Http().bindAndHandle(routes, host, port)
  httpServerFuture.onComplete{
    case Success(binding) =>
      logger.info(s"Akka server is up and bound to ${binding.localAddress}")
    case Failure(exception) =>
      logger.info(s"Akka http server failed to start",exception)
      system.terminate()
  }

  StdIn.readLine() //Let it run until someone presses return
  httpServerFuture
    .flatMap(_.unbind()) //Trigger unbinding from the port
    .onComplete(_ => system.terminate()) //Shutdown when done

}

object AkkaHttpClient extends App with HttpJsonSupport{
  implicit val actorSystem = ActorSystem("akka-http-donuts-client")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  val donutsUri = "http://localhost:5000/donuts"
  val donutsHttpRequest = HttpRequest(uri = donutsUri,method = HttpMethods.GET)

  val donutResponse = Http().singleRequest(donutsHttpRequest)
  donutResponse
    .onComplete{
      case Success(donutsResponse) =>
        println(s"Raw HttpResponse = $donutsResponse")
        val donutsFuture: Future[Donuts] = Unmarshal(donutsResponse).to[Donuts]
        val donuts: Donuts = Await.result(donutsFuture,5.second)
        println(s"Unmarshalled HttpResponse to Case Class = $donuts")
        
      case Failure(exception) => println(s"Failed to HTTP GET $donutsUri,error = ${exception.getMessage}")
    }

  Thread.sleep(3000)
  actorSystem.terminate()
}
package com.credomob.routes

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.credomob.AkkaHttpServer.{AkkaHttpRestServer, Donut, HttpJsonSupport, Ingredient}
import com.credomob.models.DonutDao
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success}

class ServerVersion extends HttpJsonSupport with LazyLogging{
  def route(): Route = {
    path("server-version"){
      get{
          val serverVersion ="1.0.0.0.0"
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`,serverVersion))
      }
    }
  }
  def routeAsJson(): Route = {
    path("server-version-json"){
      get{
        val jsonResponse = """{"app":"Vending Point Server","version":"1.1.1.0"}""".stripMargin
        complete(HttpEntity(ContentTypes.`application/json`,jsonResponse))
      }
    }
  }

  def routeAsJsonEncoding(): Route ={
    path("server-version-json-encoding"){
      get{
        val akkaServer = AkkaHttpRestServer("Vending Point Server","1.1.1.1")
        complete(akkaServer)
      }
    }
  }

}

class DonutRoutes extends HttpJsonSupport with LazyLogging{
  val donutDao = new DonutDao()

  def route(): Route ={
    path("create-donut"){
      post{
        entity(as[Donut]){ donut =>
          logger.info(s"Creating a donut = $donut")
          complete(StatusCodes.Created, s"Created a donut = $donut")
        }
      }~delete{
        complete(StatusCodes.MethodNotAllowed,"The HTTP DELETE operation is not allowed for the create-donut path.")
      }
    } ~ path("donuts"){
      get{
        onSuccess(donutDao.fetchDonuts()){ donuts =>
          complete(StatusCodes.OK, donuts)
        }
      }
    } ~ path("donuts-with-future-success-failure"){
      get{
        onComplete(donutDao.fetchDonuts()){
          case Success(donuts) => complete(StatusCodes.OK,donuts)
          case Failure(exception) => complete(s"Failed to fetch donuts =${exception.getMessage}")
        }
      }
    } ~ path("complete-with-http-response"){
      get{
        complete(HttpResponse(status = StatusCodes.OK,entity="Using an HttpResponse object"))
      }
    } ~ path("donut-with-try-httpresponse"){
      get{
        val result: HttpResponse = donutDao.tryFetchDonuts().getOrElse(donutDao.defaultResponse())
        complete(result)
      }
    } ~ path("akka-http-failwith"){
      get{
        failWith(new RuntimeException("Boom"))
      }
    } ~ path("akka-http-getresource"){
      getFromResource("error-page.html")
    } ~ path("donuts"/Segment){ donutName =>
      get{
        val result = donutDao.donutDetails(donutName)
        onSuccess(result){ donutDetail =>
          complete(StatusCodes.OK,donutDetail)
        }
      }
    } ~ path("donuts"/"stock"/ new scala.util.matching.Regex("""donut_[a-zA-Z0-9\-]*""")){ donutId =>
      get{
        complete(StatusCodes.OK,s"Looking up donut stock by donutId=$donutId")
      }
    } ~ path("donut"/"prices"){
      get{
        parameter("donutName"){ donutName =>
          val output = s"Received parameter: donutName=$donutName"
          complete(StatusCodes.OK,output)
        }
      }
    } ~ path("donut" / "bake"){
      get{
        parameters('donutName, 'topping ? "sprinkles"){ (donutName,topping) =>
          val output = s"Received parameters: donutName=$donutName and topping=$topping"
          complete(StatusCodes.OK,output)
        }
      }
    } ~ path("ingredients"){
      get{
        parameters('donutName.as[String],'priceLevel.as[Double]){ (donutName,priceLevel) =>
          val output = s"Received parameters: donutName=$donutName,priceLevel=$priceLevel"
          complete(StatusCodes.OK,output)
        }
      }
    } ~ path("bake-donuts"){
      get{
        import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers.CsvSeq
        parameter('ingredients.as(CsvSeq[String])) { ingredients =>
          val output = s"Received CSV parameter: ingredients=$ingredients"
          complete(StatusCodes.OK,output)
        }
      }
    } ~ path("ingredients-to-case-class"){
      get{
        parameters('donutName.as[String], 'priceLevel.as[Double]).as(Ingredient){ ingredient =>
          val output = s"Encoded query parameters into case class, ingredient: $ingredient"
          complete(StatusCodes.OK,output)
        }
      }
    } ~ path("request-with-headers"){
      get{
        extractRequest{ httpRequest =>
          val headers = httpRequest.headers.mkString(", ")
          complete(StatusCodes.OK,s"headers=$headers")
        }
      }
    } ~ path("multiple-segments" / Segments){segments =>
      get{
        val partA :: partB :: partC :: Nil = segments
        val output =
          """
            |Received the following Segments = $segments, with
            |partA = $partA
            |partB = $partB
            |partC = $partC
          """.stripMargin
        complete(StatusCodes.OK, output)
      }
    }
  }
}
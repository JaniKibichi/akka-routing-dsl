package com.credomob.models

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.credomob.AkkaHttpServer.{Donut, Donuts}

import scala.concurrent.Future
import scala.util.Try

class DonutDao{
  import scala.concurrent.ExecutionContext.Implicits.global

  val donutsFromDb = Vector(
    Donut("Plain Donut",1.50),
    Donut("Chocolate Donut",2.00),
    Donut("Glazed Donut",2.50)
  )

  def fetchDonuts(): Future[Donuts] = Future{
    Donuts(donutsFromDb)
  }

  def tryFetchDonuts(): Try[HttpResponse] = Try{
    throw new IllegalStateException("Boom!")
  }

  def defaultResponse(): HttpResponse = HttpResponse(status = StatusCodes.NotFound,entity="An unexpected error occurred. Please try again.")

  def donutDetails(donutName:String): Future[String] = Future{
    val someDonut = donutsFromDb.find(_.name == donutName)
    someDonut match{
      case Some(donut) => s"$donut"
      case None => s"Donut = $donutName was not found."
    }
  }
}
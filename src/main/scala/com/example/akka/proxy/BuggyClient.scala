package com.example.akka.proxy

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration._

object BuggyClient extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  val clientSettings = ClientConnectionSettings(system)
  val parserSettings = clientSettings.parserSettings.withMaxChunkSize(1).withMaxChunkExtLength(1)
  val settings = ConnectionPoolSettings(system).withConnectionSettings(clientSettings.withParserSettings(parserSettings))
  (1 to 20) foreach { _ =>
    Await.ready(
      Http().singleRequest(HttpRequest(uri = Uri("http://localhost:8080"))) map { r =>
        println(s"Request completed with: ${r.status.toString()}")
      }, 10.seconds)
  }

  system.terminate()
}

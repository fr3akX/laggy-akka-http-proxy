package com.example.akka.proxy

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.after
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
class LaggyServer(executr: HttpRequest => Future[HttpResponse])(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext) extends HttpApp {
  override protected def routes: Route = {
    get {
      extractUri { uri =>
        complete {
          after((math.random * (60 - 5) + 5).toInt.seconds, system.scheduler){
            executr(HttpRequest(uri = Uri("/api/breeds/list/all")))
          }
        }
      }
    }
  }
}

object LaggyServer extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  val exc = new RequestExecutr("dog.ceo", 443, true)
  new LaggyServer(exc.execute).startServer("0.0.0.0", 8081, system)
}
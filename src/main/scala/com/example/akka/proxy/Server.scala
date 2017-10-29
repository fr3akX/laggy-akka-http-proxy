package com.example.akka.proxy

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.ActorMaterializer

import scala.concurrent.Future

class Server(executr: HttpRequest => Future[HttpResponse])(implicit system: ActorSystem) extends HttpApp {
  override protected def routes: Route = {
    extractActorSystem { implicit system =>
      extractMaterializer { implicit mat =>
        get {
          extractUri { uri =>
            complete {
              executr(HttpRequest(uri = Uri("/api/breeds/list/all")))
            }
          }
        }
      }
    }
  }
}


object Server extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  val exc = new RequestExecutr("127.0.0.1", 8081, false)
  new Server(exc.execute).startServer("0.0.0.0", 8080, system)
}
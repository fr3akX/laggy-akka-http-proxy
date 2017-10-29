package com.example.akka.proxy

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

class RequestExecutr(host: String, port: Int, https: Boolean)(implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext) {

  private val settings = ConnectionPoolSettings(system)
    .withMaxConnections(1)
    .withMaxOpenRequests(1)
    .withMaxRetries(0)

  private val poolClientFlow =
    if (https)
      Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](host, port = port)
    else
      Http().cachedHostConnectionPool[Promise[HttpResponse]](host, port = port)

  private val queue =
    Source.queue[(HttpRequest, Promise[HttpResponse])](1, OverflowStrategy.dropNew)
      .via(poolClientFlow)
      .toMat(Sink.foreach({
        case ((Success(resp), p)) => p.success(resp)
        case ((Failure(e), p)) => p.failure(e)
      }))(Keep.left)
      .run()

  def execute(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued => responsePromise.future
      case QueueOfferResult.Dropped => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }
}
package com.mlynik

import org.scalajs.dom
import com.raquo.laminar.api.L._
import sttp.client3._
import sttp.tapir.client.sttp.SttpClientInterpreter

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object App {

  val backend = FetchBackend()
  val timerBus = EventStream.periodic(1000).flatMap(_ => fetchTime())

  def fetchTime(): EventStream[String] = {
    val serverTimeFunction = SttpClientInterpreter().toRequestThrowDecodeFailures(Endpoints.serverTime, Some(uri"http://localhost:9000"))

    EventStream.fromFuture(backend.send(serverTimeFunction(())).map(_.body.toOption.get))
  }

  def main(args: Array[String]): Unit = {
    val containerNode = dom.document.querySelector("#app")
    render(
      containerNode,
      div(
        p("This is an app"),
        p("Laminar is quite cool"),
        sectionTag(
          child <-- timerBus.map {
            time => p(time)
          }
        )
      )
    )
  }
}

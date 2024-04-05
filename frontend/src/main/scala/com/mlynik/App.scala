package com.mlynik

import org.scalajs.dom
import com.raquo.laminar.api.L._
import sttp.client3._
import sttp.tapir.client.sttp.SttpClientInterpreter
import frontroute._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Date


object NavBar {
  def apply() = {
    div(
      button(
        href := "/time",
        onClick.preventDefault --> { _ => BrowserNavigation.pushState(url = "/time") },
        "Time"
      ),
      button(
        href := "/books",
        onClick.preventDefault --> { _ => BrowserNavigation.pushState(url = "/books") },
        "Books"
      )
    )
  }
}

object Time {

  val backend = FetchBackend()
  val timerBus = EventStream.periodic(1000).flatMap(_ => fetchTime())

  def fetchTime(): EventStream[String] = {
    val serverTimeFunction = SttpClientInterpreter().toRequestThrowDecodeFailures(Endpoints.serverTime, Some(uri"/"))

    EventStream.fromFuture(backend.send(serverTimeFunction(())).map(_.body.toOption.get))
  }

  def apply() = {
    div(
      p("This is an app"),
      p("Laminar is quite cool - docker is also cool!"),
      sectionTag(
        child <-- timerBus.map {
          time => p(new Date(time.toLong).toString())
        }
      )
    )
  }
}

object App {

  val app = div(
    mainTag(
      NavBar(),
      routes(
        div(
          pathEnd {
            p("Laminar app demo")
          },
          path("time").apply {
            Time()
          },
          path("books").apply {
            Books()
          },
          noneMatched {
            div(
              "Not found"
            )
          }
        )
      )
    ),
  ).amend(LinkHandler.bind) // for internal links

  def main(args: Array[String]): Unit = {
    val containerNode = dom.document.querySelector("#app")
    render(
      containerNode,
      app
    )
  }
}

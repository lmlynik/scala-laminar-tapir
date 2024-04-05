package com.mlynik

import com.raquo.laminar.api.L._
import sttp.client3._
import sttp.tapir.client.sttp.SttpClientInterpreter

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object Books {

  val backend = FetchBackend()
  val bookBus = EventBus[List[Book]]()

  case class BookFormState(
                            title: String = "",
                            author: String = "",
                            year: Int = 2024
                          )

  private val stateVar = Var(BookFormState())

  private val titleWriter = stateVar.updater[String]((state, title) => state.copy(title = title))

  private val authorWriter = stateVar.updater[String]((state, author) => state.copy(author = author))

  private val yearWriter = stateVar.updater[Int]((state, year) => state.copy(year = year))

  private val submitter = Observer[BookFormState] {
    state =>
      val bookAddFunction = SttpClientInterpreter().toRequestThrowDecodeFailures(Endpoints.booksAdd, Some(uri"/"))
      backend.send(bookAddFunction(Book(title = state.title, author = state.author, year = state.year))).flatMap { _ =>
        fetchBooks()
      }
  }

  def fetchBooks() = {
    val bookQueryFunction = SttpClientInterpreter().toRequestThrowDecodeFailures(Endpoints.booksListing, Some(uri"/"))

    backend.send(bookQueryFunction(BooksQuery("Werther", 1774))).map { response =>
      response.body match {
        case Left(error) => println(s"Error: $error")
        case Right(books) => bookBus.emit(books)
      }
    }
  }

  def apply() = {
    div(
      onMountCallback(_ => fetchBooks()),
      p("Books"),
      div(
        form(
          onSubmit
            .preventDefault
            .mapTo(stateVar.now()) --> submitter,
          label("Title"),
          input(
            typ := "text",
            controlled(
              value <-- stateVar.signal.map(_.title),
              onInput.mapToValue --> titleWriter
            )
          ),
          label("Author"),
          input(
            typ := "text",
            controlled(
              value <-- stateVar.signal.map(_.author),
              onInput.mapToValue --> authorWriter
            )
          ),
          label("Year"),
          input(
            typ := "number",
            controlled(
              value <-- stateVar.signal.map(_.year.toString),
              onInput.mapToValue.map(_.toInt) --> yearWriter
            )
          ),
          button(typ("submit"), "Add book")

        )
      ),
      ul(
        children <-- bookBus.events.map { books =>
          books.map { book =>
            li(
              p(book.title),
              p(book.author),
              p(book.year),
              hr()
            )
          }
        }

      )
    )
  }
}
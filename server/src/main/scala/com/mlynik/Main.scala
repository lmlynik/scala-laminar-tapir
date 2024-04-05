package com.mlynik

import sttp.tapir._
import sttp.tapir.files.staticResourcesGetServerEndpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.jdkhttp._
import sttp.tapir.swagger.bundle.SwaggerInterpreter



object Main extends App {


  var books = Seq(Book(
    "The Sorrows of Young Werther",
    "Johann Wolfgang von Goethe",
    1774
  ))

  val public = staticResourcesGetServerEndpoint[Id]("public")(
    this.getClass.getClassLoader,
    "public"
  )

  val serverTime = Endpoints.serverTime
    .handle(_ => Right(System.currentTimeMillis().toString))

  val bookAdd = Endpoints.booksAdd
    .handle { book =>
      println(s"Adding book $book")
      books = books :+ book
      println(s"Books are now $books")
      Right(())
    }

  val bookQuery = Endpoints.booksListing
    .handle { query =>
      val filtered = books  // TODO add filtering .filter(book => book.year == query.year && book.title.contains(query.genre))
      Right(filtered.toList)
    }

  val swaggerEndpoints: List[ServerEndpoint[Any, Id]] =
    SwaggerInterpreter().fromEndpoints(List(
      Endpoints.serverTime,
      Endpoints.booksListing,
      Endpoints.booksAdd,
      public.endpoint
    ), "My App", "1.0")

  println("Starting server...")
  val server: HttpServer =
    JdkHttpServer()
      .port(9000)
      .addEndpoint(serverTime)
      .addEndpoint(bookAdd)
      .addEndpoint(bookQuery)
      .addEndpoints(swaggerEndpoints)
      .addEndpoint(public)
      .start()

  println("Server started")

}
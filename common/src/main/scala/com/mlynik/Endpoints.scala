package com.mlynik

import sttp.tapir._
import sttp.tapir.json.zio._
import sttp.tapir.generic.auto._
import zio.json._
import Book._

case class BooksQuery(genre: String, year: Int)

case class Book(title: String, author: String, year: Int)

object Book {
  implicit val bookCodec: JsonCodec[Book] = DeriveJsonCodec.gen[Book]
}

object Endpoints {
  val serverTime: Endpoint[Unit, Unit, Unit, String, Any] = endpoint.get
    .in("api" / "time")
    .out(stringBody)

  val booksListing: Endpoint[Unit, BooksQuery, Unit, List[Book], Any] = endpoint.get
    .tag("books")
    .in(("api" / "books" / path[String]("genre") / path[Int]("year")).mapTo[BooksQuery])
    .out(jsonBody[List[Book]].description("List of books"))

  val booksAdd: Endpoint[Unit, Book, Unit, Unit, Any] = endpoint.post
    .tag("books")
    .in("api" / "books")
    .in(jsonBody[Book])
}

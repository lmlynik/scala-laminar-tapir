package com.mlynik

import sttp.tapir._
import sttp.tapir.files.staticResourcesGetServerEndpoint
import sttp.tapir.server.jdkhttp._


object Main {
  def main(args: Array[String]): Unit = {

    val public = staticResourcesGetServerEndpoint[Id]("public")(
      this.getClass.getClassLoader,
      "public"
    )

    val serverTime = Endpoints.serverTime
      .handle(_ => Right("The time is: " + System.currentTimeMillis()))

    val server: HttpServer =
      JdkHttpServer()
        .port(9000)
        .addEndpoint(serverTime)
        .addEndpoint(public)
        .start()

  }
}
package com.mlynik

import sttp.tapir._

object Endpoints {
  val serverTime: Endpoint[Unit, Unit, Unit, String, Any] = endpoint.get
    .in("time")
    .out(stringBody)
}

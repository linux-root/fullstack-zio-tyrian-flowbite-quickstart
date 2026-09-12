package com.example.tyrianflowbitequickstart.app

import com.example.tyrianflowbitequickstart.config.AppConfig
import com.example.tyrianflowbitequickstart.http.DefaultRoutes.*
import com.example.tyrianflowbitequickstart.service.JWTIssuer
import com.example.tyrianflowbitequickstart.service.JWTVerifier
import com.example.tyrianflowbitequickstart.services.RandomQuotes
import zio.*
import zio.Console.*
import zio.http.*
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {

  override def run =
    Server
      .serve(public ++ authenticated)
      .provide(
        Server.defaultWithPort(8080),
        JWTVerifier.live,
        JWTIssuer.live,
        AppConfig.live,
        RandomQuotes.live
      )

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

}

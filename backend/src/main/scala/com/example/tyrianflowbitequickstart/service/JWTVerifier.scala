package com.example.tyrianflowbitequickstart.service

import com.example.tyrianflowbitequickstart.common.util.JwtHelper
import com.example.tyrianflowbitequickstart.common.util.JwtHelper.UserClaim
import com.example.tyrianflowbitequickstart.config.AppConfig
import zio.*

import JWTVerifier.*

trait JWTVerifier {
  def isValid(token: String): UIO[Boolean] = decode(token).fold(_ => false, _ => true)
  def decode(token: String): IO[String, UserClaim]
}

object JWTVerifier {
  val live = ZLayer.fromFunction((config: AppConfig) =>
    new JWTVerifier {
      private val secret = config.secret
      override def decode(token: String): IO[String, UserClaim] =
        ZIO.fromEither(JwtHelper.decode(token, secret))
    }
  )

  def decode(token: String): ZIO[JWTVerifier, String, UserClaim] =
    ZIO.serviceWithZIO[JWTVerifier](_.decode(token))

}

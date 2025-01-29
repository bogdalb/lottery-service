package auth

import com.typesafe.config.{Config, ConfigFactory}

case class JwtAuthConfiguration(secretKey: String, expirationTimeInSeconds: Long)

object JwtAuthConfiguration {
  def fromConfig(config: Config): JwtAuthConfiguration = {
    val jwtAuthConfig: Config = ConfigFactory.load().getConfig("auth.jwt")
    val secretKey: String = jwtAuthConfig.getString("secret-key")
    val expirationTime: Long = jwtAuthConfig.getLong("expiration-time")
    JwtAuthConfiguration(secretKey, expirationTime)
  }
}
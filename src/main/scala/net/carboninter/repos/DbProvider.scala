package net.carboninter.repos

import com.typesafe.config.Config
import reactivemongo.api.AsyncDriver

import scala.concurrent.ExecutionContext

class DbProvider(driver: AsyncDriver, config: Config)(implicit executionContext: ExecutionContext) {
  lazy val connection = driver.connect(config.getString("database.url"))
  lazy val db = for {
    conn <- connection
    db <- conn.database(config.getString("database.name"))
  } yield db
}

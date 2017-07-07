package asynchorswim.aurora

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import scala.util.Try

trait ConfigSupport { this: App =>
  def name: Symbol

  lazy val globalConfig: Config = {
    val baseConfig = ConfigFactory.load()
    Try[Config](ConfigFactory
      .parseFile(
        new File(getClass
          .getClassLoader
          .getResource(baseConfig.getString(s"${name.name}.secureConfigFile"))
          .getFile))
      .withFallback(baseConfig))
      .getOrElse(baseConfig)
  }

  implicit lazy val config = globalConfig.getConfig(name.name)
}

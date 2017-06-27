package asynchorswim.aurora

import com.typesafe.config.{Config, ConfigFactory}

trait ConfigSupport { this: App =>
  def name: Symbol

  val baseConfig: Config = ConfigFactory.load()
  implicit val config: Config = baseConfig.getConfig(name.name)
}

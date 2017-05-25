package asynchorswim

import java.util.concurrent.TimeUnit

import scala.language.implicitConversions

package object aurora {

  implicit def toScalaDuration(d: java.time.Duration): scala.concurrent.duration.FiniteDuration =
    scala.concurrent.duration.FiniteDuration(d.toMillis, TimeUnit.MILLISECONDS)

}
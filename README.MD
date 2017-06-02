Fusion     ![Build Status](https://travis-ci.org/asynchorswim/fusion.svg?branch=master)
=======
Fusion provides state management and flexible persistence for Scala/Akka.

Installation
------------

_SBT_

Add the following to your `build.sbt` file:

`libraryDependencies += "net.asynchorswim" %% "fusion" % "1.0.0"`

_Gradle_

Add the following to ytour `build.gradle` file within the `dependencies` section:

`compile 'net.asynchorswim:fusion_2,12:1.0.0'`

Usage
-----
The first step in using fusion is to model your domain in terms of one or more
`Entity`. An entity defines state and behavior and will map to either a single
Akka actor or a collection of actors.

For example, the following defines an entity that can be used to count the
occurance of words in a sentence:

```scala
final case class WordCounter(count: Int) extends Entity[WordCounter] {
  override def receive(implicit ctx: Context) = {
    case CountWord(_) => applying(CountIncremented)
    case QueryCount(_) => reply(couhnt); NoOp
  }

  override def applyEvent = {
    case CountIncremented => copy(count = count + 1)
  }
}

object WordCounter extends EntityCompanion[WordCounter] {
  val empty = WordCounter(0)

  final case class CountWord(word: String) extends Command with ShardingId { def id = word }
  final case class QueryCount(word: String) extends ShardingId { def id = word }

  case object CountIncremented extends Event
}
```

This enntity definition can then be used to create an Aggregate Root of word counters as follows:

```scala
implicit val timeProvider = SystemTimeProvider
implicit val system = ActorSystem()
implicit val timeout = Timeout(1 second)

:

val words = system.actorOf(AggregateRoot.props[WordCounter](TransientEntity))

"The rain in Spain falls mainly in the plain"
  .split(" ")
  .map(w => WordCounter.CountWord(w.toUpperCase))
  .foreach(c => words ! c)

val c = words ? WordCounter.QueryCount("IN") // Returns Future[Int] that completes with "2"
```

Persistence Mechanisms
----------------------

* `TransientEntity`
* `CommandSourcedEntity`
* `EventSourcedEntity`
* `PersistedEntity`

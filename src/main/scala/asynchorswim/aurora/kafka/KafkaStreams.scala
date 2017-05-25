package asynchorswim.aurora.kafka

import akka.actor.ActorSystem
import akka.kafka.scaladsl._
import akka.kafka.{ProducerSettings, _}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.language.postfixOps

class KafkaStreams(config: Config)(implicit system: ActorSystem, mat: ActorMaterializer) {
  private val bootstrapServers = config.getString("akka.kafka.bootstrapServers")

  private val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers(bootstrapServers)

  private val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrapServers)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def source(topics: (String, Long)*) = {
    val partition = 0
    val subscription = Subscriptions.assignmentWithOffset(topics.toSeq.map{ case (t: String, o: Long) => new TopicPartition(t, partition) -> o}.toMap)
    Consumer.plainSource(consumerSettings, subscription)
  }
  def sink =  Producer.plainSink(producerSettings)
}

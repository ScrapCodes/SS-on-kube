/*
 *
 * Streaming Benchmark
 *
 * Copyright IBM.
 *
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package org.codait.sb.it.kafka

import org.codait.sb.it.{TestBase, TestSetup => ts}
import org.codait.sb.util.ClusterUtils
import org.scalatest.concurrent.Eventually.{eventually, interval, timeout}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._

class KafkaSanityTest extends TestBase {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  test("create and fetch Kafka topics.") {
    val pods = ts.getKafkaCluster.getPods
    logger.info(s"Found kafka pods: ${pods.map(_.getMetadata.getName).mkString(", ")}.")
    val pod1 = pods.head
    val pod2 = pods.last
    val rf: Integer = ts.getKafkaCluster.clusterConfig.replicaSize

    def deleteCommand(topic: String) =
      s"kafka-topics.sh --delete --topic $topic --zookeeper ${ts.getZkAddress}"

    eventually(timeout(3.minutes), interval(30.seconds)) {

      val topic = "test" + ts.testingPrefix

      val createTopicCommand =
        s"kafka-topics.sh --create" +
          s" --zookeeper ${ts.getZkAddress}" +
          s" --replication-factor $rf" +
          s" --partitions $rf" +
          s" --topic $topic"

      val (result1, _) = ClusterUtils.execCommand(pod1, createTopicCommand, ts.kubernetesClient,
        chkResult = "Created")
      assert(result1.contains(s"""Created topic "$topic"."""),
        s"Topic creation failed. \nOutput:$result1")
      logger.info(s"Successfully created topic $topic on the kafka node: ${pod1.getMetadata.getName}.")
      // Get a topic description.
      val getTopicCommand = s"kafka-topics.sh --describe --topic $topic --zookeeper ${ts.getZkAddress}"

      val (result2: String, _) =
        ClusterUtils.execCommand(pod2, command = getTopicCommand,
          ts.kubernetesClient,
          chkResult = s"ReplicationFactor:$rf")
      val r, _ = ClusterUtils.execCommand(pod1, deleteCommand(topic), ts.kubernetesClient)
      logger.debug(s"Trying to delete the topic $topic. \n Output: $r")
      assert(!result2.contains("ERROR") || result2.contains("Exception"),
        s"Output should not contain ERROR or Exception, Actual output: $result2")
      assert(result2.contains(s"ReplicationFactor:$rf"),
        s"Replication factor should match the requested count. Actual output: $result2")
    }
  }

  test(s"produce and consume a message from kafka topic: t${ts.testingPrefix}") {
    val topic = s"t${ts.testingPrefix}"
    val pods = ts.getKafkaCluster.getPods
    val brokerAddress = ts.getKafkaCluster.serviceAddresses.head.internalAddress.get.toString
    logger.info(s"Found kafka pods: ${pods.map(_.getMetadata.getName).mkString(", ")}.")
    val pod1 = pods.head
    val pod2 = pods.last
    val sendTestData = s"kafkatest-$topic"
    val sendCommand =
      s"echo '$sendTestData' | kafka-console-producer.sh --topic $topic --broker-list $brokerAddress"
    val consumeCommand =
      s"kafka-console-consumer.sh" +
        s" --topic $topic" +
        s" --bootstrap-server $brokerAddress" +
        s" --from-beginning" +
        s" --timeout-ms 2000"

    eventually(timeout(2.minutes), interval(10.seconds)) {
      ClusterUtils.execCommand(pod1, sendCommand, ts.kubernetesClient)
      val (result: String, s2) =
        ClusterUtils.execCommand(pod1, consumeCommand, ts.kubernetesClient, chkResult = sendTestData)
      assert(result.contains(sendTestData))
    }
  }

}
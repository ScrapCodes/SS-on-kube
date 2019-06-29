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

import java.util.UUID

import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.codait.sb.deploy.kafka.{KafkaCluster, KafkaClusterConfig}
import org.codait.sb.deploy.zookeeper.{ZKCluster, ZKClusterConfig}
import org.codait.sb.util.{ClusterUtils, SBConfig}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.slf4j.{Logger, LoggerFactory}
import org.scalatest.concurrent.Eventually.{eventually, interval, timeout}

import scala.concurrent.duration._


class KafkaSanityTest extends FunSuite with BeforeAndAfterAll {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  private lazy val kubernetesClient = new DefaultKubernetesClient()
    .inNamespace(SBConfig.NAMESPACE)

  private val testingPrefix = s"t${UUID.randomUUID().toString.takeRight(5)}"

  private val zkCluster = new ZKCluster(ZKClusterConfig(clusterPrefix = testingPrefix,
    replicaSize = 3, "default"))

  private def zookeeperAddress = zkCluster.serviceAddresses("zookeeper")

  private lazy val kafkaCluster = new KafkaCluster(KafkaClusterConfig(clusterPrefix = testingPrefix,
    replicaSize = 3, zookeeperAddress, startTimeoutSeconds= 120, "default"))

  override def beforeAll() {
    super.beforeAll()
    zkCluster.start()
    assert(zkCluster.isRunning(20),
      "Zookeeper service is required before kafka service can run.")
    kafkaCluster.start()
    assert(kafkaCluster.isRunning(20),
      "Kafka service should be up and running.")
    kubernetesClient
  }

  test("create and fetch Kafka topics.") {
    val pods = kafkaCluster.getPods
    logger.info(s"Found kafka pods: ${pods.map(_.getMetadata.getName).mkString(", ")}.")
    val pod1 = pods.head
    val pod2 = pods.last
    val rf: Integer = kafkaCluster.clusterConfig.replicaSize
    def deleteCommand(topic: String) =
      s"kafka-topics.sh --delete --topic $topic --zookeeper $zookeeperAddress"
    eventually(timeout(3.minutes), interval(30.seconds)) {

      val topic = "test" + testingPrefix

      val createTopicCommand =
        s"kafka-topics.sh --create" +
          s" --zookeeper $zookeeperAddress" +
          s" --replication-factor $rf" +
          s" --partitions $rf" +
          s" --topic $topic"

      val (result1, _) = ClusterUtils.execCommand(pod1, createTopicCommand, kubernetesClient,
        chkResult = "Created")
      assert(result1.contains(s"""Created topic "$topic"."""),
        s"Topic creation failed. \nOutput:$result1")
      logger.info(s"Successfully created topic $topic on the kafka node: ${pod1.getMetadata.getName}.")
      // Get a topic description.
      val getTopicCommand = s"kafka-topics.sh --describe --topic $topic --zookeeper $zookeeperAddress"

      val (result2: String, _) =
        ClusterUtils.execCommand(pod2, command = getTopicCommand,
          kubernetesClient,
          chkResult = s"ReplicationFactor:$rf")
      val r, _ = ClusterUtils.execCommand(pod1, deleteCommand(topic), kubernetesClient)
      logger.debug(s"Trying to delete the topic $topic. \n Output: $r")
      assert(!result2.contains("ERROR") || result2.contains("Exception"),
        s"Output should not contain ERROR or Exception, Actual output: $result2")
      assert(result2.contains(s"ReplicationFactor:$rf"),
        s"Replication factor should match the requested count. Actual output: $result2")
    }
  }

  override def afterAll(): Unit = {
    zkCluster.stop()
    kafkaCluster.stop()
  }
}
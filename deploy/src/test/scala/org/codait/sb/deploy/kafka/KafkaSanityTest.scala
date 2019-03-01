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

package org.codait.sb.deploy.kafka

import java.util.UUID

import org.codait.sb.deploy.{zookeeper => zk}
import org.codait.sb.util.{SBConfig, SanityTestUtils}
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.scalatest.concurrent.Eventually.{eventually, interval, timeout}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.concurrent.duration._


class KafkaSanityTest extends FunSuite with BeforeAndAfterAll {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  private lazy val kubernetesClient = new DefaultKubernetesClient()
    .inNamespace(SBConfig.NAMESPACE)
  private val zookeeperAddress =
    s"${zk.Constants.ZK_CLIENT_SERVICE_NAME}:${zk.Constants.ZK_CLIENT_PORT}"

  override def beforeAll() {
    kubernetesClient
    val serviceList = kubernetesClient.services().list().getItems.asScala
    assert(serviceList.exists(_.getMetadata.getName ==
      Services.brokerService.getMetadata.getName), "Cluster should be started before sanity tested.")

    val ssName = KafkaStatefulSet.statefulSet.getMetadata.getName
    val ss = kubernetesClient.apps().statefulSets().withName(ssName).get()
    SanityTestUtils.waitForClusterUpAndReady(kubernetesClient, ss, timeoutSeconds = 20)
    logger.info("Kafka cluster is up and ready for testing.")
  }

  test("create and fetch Kafka topics.") {
    val zkPods = SanityTestUtils.getPodsWhenReady(kubernetesClient, Services.labels)
    logger.info(s"Found kafka pods: ${zkPods.map(_.getMetadata.getName).mkString(", ")}.")
    val pod1 = zkPods.head
    val pod2 = zkPods.last
    val rf: Integer = KafkaStatefulSet.statefulSet.getSpec.getReplicas
    def deleteCommand(topic: String) =
      s"kafka-topics.sh --delete --topic $topic --zookeeper $zookeeperAddress"
    eventually(timeout(3.minutes), interval(30.seconds)) {

      val topic = "test" + UUID.randomUUID().toString.takeRight(5)

      val createTopicCommand =
        s"kafka-topics.sh --create" +
          s" --zookeeper $zookeeperAddress" +
          s" --replication-factor $rf" +
          s" --partitions $rf" +
          s" --topic $topic"

      val (result1, _) = SanityTestUtils.execCommand(pod1, createTopicCommand, kubernetesClient,
        chkResult = "Created")
      assert(result1.contains(s"""Created topic "$topic"."""),
        s"Topic creation failed. \nOutput:$result1")
      logger.info(s"Successfully created topic $topic on the kafka node: ${pod1.getMetadata.getName}.")
      // Get a topic description.
      val getTopicCommand = s"kafka-topics.sh --describe --topic $topic --zookeeper $zookeeperAddress"

      val (result2: String, _) =
        SanityTestUtils.execCommand(pod2, command = getTopicCommand,
          kubernetesClient,
          chkResult = s"ReplicationFactor:$rf")
      val r, _ = SanityTestUtils.execCommand(pod1, deleteCommand(topic), kubernetesClient)
      logger.debug(s"Trying to delete the topic $topic. \n Output: $r")
      assert(!result2.contains("ERROR") || result2.contains("Exception"),
        s"Output should not contain ERROR or Exception, Actual output: $result2")
      assert(result2.contains(s"ReplicationFactor:$rf"),
        s"Replication factor should match the requested count. Actual output: $result2")
    }
  }
}
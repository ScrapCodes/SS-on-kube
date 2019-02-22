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

package com.ibm.bench.deploy.kafka

import java.util.UUID

import com.ibm.bench.deploy.SanityTestUtils
import com.ibm.bench.util.SBConfig
import com.ibm.bench.deploy.{zookeeper => zk}
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.scalatest.concurrent.Eventually.{eventually, interval, timeout}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.collection.JavaConverters._
import scala.concurrent.duration._


class KafkaSanityTest extends FunSuite with BeforeAndAfterAll {

  private lazy val kubernetesClient = new DefaultKubernetesClient()
    .inNamespace(SBConfig.NAMESPACE)

  override def beforeAll() {
    kubernetesClient
    val serviceList = kubernetesClient.services().list().getItems.asScala
    assert(serviceList.exists(_.getMetadata.getName ==
      Services.brokerService.getMetadata.getName), "Cluster should be started before sanity tested.")

    val ssName = KafkaStatefulSet.statefulSet.getMetadata.getName
    val ss = kubernetesClient.apps().statefulSets().withName(ssName).get()
    SanityTestUtils.waitForClusterUpAndReady(kubernetesClient, ss, timeoutSeconds = 20)
  }

  test("create and fetch Kafka topics.") {
    val zkPods = SanityTestUtils.getPodsWhenReady(kubernetesClient, Services.labels)
    val pod1 = zkPods.head
    val pod2 = zkPods.last
    val rf: Integer = KafkaStatefulSet.statefulSet.getSpec.getReplicas
    val zookeeperAddress = s"${zk.Constants.ZK_CLIENT_SERVICE_NAME}:${zk.Constants.ZK_CLIENT_PORT}"

    eventually(timeout(3.minutes), interval(30.seconds)) {

      val topic = "test" + UUID.randomUUID().toString.takeRight(5)

      val createTopicCommand =
        s"kafka-topics.sh --create " +
          s"--zookeeper $zookeeperAddress" +
          s" --replication-factor $rf" +
          s" --partitions $rf" +
          s" --topic $topic"

      val (result1, _) = SanityTestUtils.execCommand(pod1, createTopicCommand, kubernetesClient)
      assert(result1.contains(s"""Created topic "$topic"."""),
        s"Topic creation failed. \nOutput$result1")

      // Get a topic description.
      val getTopicCommand = s"kafka-topics.sh --describe $topic --zookeeper $zookeeperAddress"

      val (result2: String, _) =
        SanityTestUtils.execCommand(pod2, command = getTopicCommand,
          kubernetesClient,
          chkResult = s"ReplicationFactor:$rf")

      assert(!result2.contains("ERROR") || result2.contains("Exception"),
        s"Output should not contain ERROR or Exception, Actual output: $result2")
      assert(result2.contains(s"ReplicationFactor:$rf"),
        s"Replication factor should match the requested count. Actual output: $result2")
    }
  }
}
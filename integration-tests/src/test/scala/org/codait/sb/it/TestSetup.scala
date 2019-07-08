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

package org.codait.sb.it

import java.util.UUID

import io.fabric8.kubernetes.client.{DefaultKubernetesClient, KubernetesClient}
import org.codait.sb.deploy.kafka.{KafkaCluster, KafkaClusterConfig}
import org.codait.sb.deploy.zookeeper.{ZKCluster, ZKClusterConfig}
import org.codait.sb.util.SBConfig
import org.scalatest.FunSuite
import org.slf4j.{Logger, LoggerFactory}

class TestSetup extends FunSuite {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  import TestSetup._

  private var started: Boolean = false
  private val zkCluster = new ZKCluster(ZKClusterConfig(clusterPrefix = testingPrefix,
    replicaSize = 3, startTimeoutSeconds = 120, "default"))

  private def zookeeperAddress = instance.zkCluster.serviceAddresses("zookeeper")

  private lazy val kafkaCluster = new KafkaCluster(KafkaClusterConfig(clusterPrefix = testingPrefix,
    replicaSize = 3, getZkAddress, startTimeoutSeconds = 240, "default"))

  private def startClusters(): Unit = synchronized {
    if (!started) {
      instance.started = true
      instance.zkCluster.start()
      instance.kafkaCluster.start()
    } else {
      logger.warn("All the cluster are already started.")
    }
  }

  private def stop(): Unit = {
    if (started) {
      instance.started = false
      instance.zkCluster.stop()
      instance.kafkaCluster.stop()
    } else {
      logger.warn("Cluster not started.")
    }
  }

  test("Zookeeper service is running.") {
    assert(instance.zkCluster.isRunning(5))
  }

  test("Kafka service is up and running.") {
    assert(instance.kafkaCluster.isRunning(5))
  }

}

object TestSetup {
  val testingPrefix: String = s"t${UUID.randomUUID().toString.takeRight(5)}"

  lazy val kubernetesClient: KubernetesClient = new DefaultKubernetesClient()
    .inNamespace(SBConfig.NAMESPACE)

  private val instance = new TestSetup
  instance.startClusters()

  private def apply: TestSetup = {
    instance
  }

  Runtime.getRuntime.addShutdownHook({
    new Thread(() =>
      instance.stop())
  })

  def getKafkaCluster: KafkaCluster = instance.kafkaCluster

  def getZKCluster: ZKCluster = instance.zkCluster

  def getZkAddress: String = instance.zookeeperAddress
}
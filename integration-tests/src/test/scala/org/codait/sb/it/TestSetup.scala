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

import org.codait.sb.deploy.Address
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
    replicaSize = 3, startTimeoutSeconds = 200, "default", serviceAccount = "spark"))

  private def zookeeperAddress: Address =
    instance.zkCluster.serviceAddresses.head.internalAddress.get

  private lazy val kafkaCluster = new KafkaCluster(KafkaClusterConfig(clusterPrefix = testingPrefix,
    replicaSize = 3, getZkAddress, startTimeoutSeconds = 300, "default", serviceAccount = "spark"))

  private def startClusters(): Unit = synchronized {
    if (!started) {
      instance.started = true
      instance.zkCluster.start()
      assert(instance.zkCluster.isRunning(360),
        "Could not start zookeeper.")
      instance.kafkaCluster.start()
    } else {
      logger.warn("All the cluster are already started.")
    }
  }

  private def stop(): Unit = synchronized {
    if (started) {
      instance.started = false
      // TODO: Is there a way, that we do not need to block here.
      instance.zkCluster.stop()
      instance.kafkaCluster.stop()
    } else {
      logger.warn("Kafka and Zookeeper cluster not started.")
    }
  }

}

object TestSetup {
  val testingPrefix: String = s"t${UUID.randomUUID().toString.takeRight(5)}"

  lazy val kubernetesClient: KubernetesClient = new DefaultKubernetesClient()
    .inNamespace(SBConfig.NAMESPACE)

  private val instance = new TestSetup

  def init(): Unit = {
    instance.startClusters()
  }

  private def apply: TestSetup = {
    instance
  }

  Runtime.getRuntime.addShutdownHook({
    new Thread(() =>
      instance.stop())
  })

  def getKafkaCluster: KafkaCluster = instance.kafkaCluster

  def getZKCluster: ZKCluster = instance.zkCluster

  def getZkAddress: Address = instance.zookeeperAddress
}
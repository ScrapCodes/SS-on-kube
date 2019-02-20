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

import com.ibm.bench.deploy.SanityTestUtils
import com.ibm.bench.util.SBConfig
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

object KafkaCluster {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def serviceList =
    kubernetesClient.services().withLabels(Services.labels).list().getItems.asScala

  //TODO: Have a proper client factory, which loads all configuration specified by the user.
  private lazy val kubernetesClient = new DefaultKubernetesClient()
    .inNamespace(SBConfig.NAMESPACE)

  def start(): Unit = {
    kubernetesClient.services().createOrReplace(Services.brokerService)
    logger.info("Started services.")

    assert(serviceList.size == 1, "Kafka services should be started.")
    val ss = kubernetesClient.apps()
      .statefulSets()
      .createOrReplace(KafkaStatefulSet.statefulSet)

    SanityTestUtils.waitForClusterUpAndReady(client = kubernetesClient, ss)

    val pods = kubernetesClient.pods().withLabels(Services.labels)
      .list().getItems.asScala.map(_.getMetadata.getName)
    logger.info(
      s"""
         |Kafka cluster started.
         |
         |Pods Names: ${pods.toList.mkString("\n")}
         |
         |Services: ${serviceList.toList.mkString("\n")}
       """.stripMargin)
  }

  def stop(): Unit = {
    kubernetesClient.services().delete(serviceList.asJava)
    kubernetesClient.apps().statefulSets().delete(KafkaStatefulSet.statefulSet)
  }
}

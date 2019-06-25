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

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

import io.fabric8.kubernetes.api.model.Pod

import org.codait.sb.deploy.Cluster
import org.codait.sb.util.ClusterUtils

import org.slf4j.{Logger, LoggerFactory}


class KafkaCluster(override val clusterConfig: KafkaClusterConfig)  extends Cluster {
  private val prefix = clusterConfig.clusterPrefix

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def serviceList =
    kubernetesClient.services().withLabels(Services.labels(prefix)).list().getItems.asScala

  override val serviceAddress: String =
    s"${Helpers.kafkaServiceName(prefix)}:${Constants.KAFKA_BROKER_PORT}"

  private val podsAssigned = ArrayBuffer[Pod]()

  def start(): Unit = {
    kubernetesClient.services().createOrReplace(Services.brokerService(prefix))
    logger.info("Starting kafka services.")

    assert(serviceList.size == 1, "'Start kafka services' should be submitted.")
    val ss = kubernetesClient.apps()
      .statefulSets()
      .createOrReplace(KafkaStatefulSet.statefulSet(clusterConfig))
    logger.info(s"Stateful set: ${ss.getMetadata.getName} submitted.")
    ClusterUtils.waitForClusterUpAndReady(client = kubernetesClient, ss, timeoutSeconds = 120)

    val pods = kubernetesClient.pods().withLabels(Services.labels(prefix))
      .list().getItems.asScala

    podsAssigned.appendAll(pods)

    logger.info(
      s"""
         |Kafka cluster started.
         |
         |Pods Names: ${pods.map(_.getMetadata.getName).mkString("\n")}
         |
         |Services: ${serviceList.map(_.getMetadata.getName).mkString("\n")}
       """.stripMargin)
  }

  /**
    * Determine whether the Kafka Cluster is up and running, in the given timeout.
    *
    * @param timeoutSeconds Total timeout.
    * @return Returns false in both cases, one it is determined the cluster is not running or,
    *         it could not be ascertained in the given timeout.
    */
  def isRunning(timeoutSeconds: Int = 5): Boolean = {
    val serviceList = kubernetesClient.services().list().getItems.asScala

    val isBrokerServiceUp = serviceList.exists(_.getMetadata.getName ==
      Services.brokerService(prefix).getMetadata.getName)

    val ssName = KafkaStatefulSet.statefulSet(clusterConfig).getMetadata.getName
    val ss = kubernetesClient.apps().statefulSets().withName(ssName).get()

    isBrokerServiceUp && ClusterUtils.waitForClusterUpAndReady(kubernetesClient, ss,
       timeoutSeconds, throwException = false)
  }

  def stop(): Unit = {
    kubernetesClient.services().delete(serviceList.asJava)
    kubernetesClient.apps().statefulSets()
      .delete(KafkaStatefulSet.statefulSet(clusterConfig))
  }

  override def getPods: Seq[Pod] = podsAssigned
}

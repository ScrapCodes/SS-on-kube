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
    Cluster.k8sClient.services()
      .withLabels(Services.labels(prefix)).list().getItems.asScala

  private def brokerService() = {
    serviceList.find(_.getMetadata.getName ==
      Services.brokerService(prefix).getMetadata.getName)
  }

  override def serviceAddresses: Map[String, String] = {
    assert(isRunning(5), "Kafka service is not running.")
    Map("kafka-broker-internal" ->
      s"${Helpers.kafkaServiceName(prefix)}:${Constants.KAFKA_BROKER_PORT}",
      "kafka-broker-external" ->
        (s"${KafkaStatefulSet.statefulSet(clusterConfig).getMetadata.getName}-0" +
          s".${Helpers.kafkaServiceName(prefix)}.${clusterConfig.kubernetesNamespace}.svc.cluster.local:" +
          s"${Services.getNodePort(brokerService().get)}"))
  }

  private val podsAssigned = ArrayBuffer[Pod]()

  override def start(): Unit = {
    Cluster.k8sClient.services().createOrReplace(Services.brokerService(prefix))
    logger.info("Starting kafka services.")

    assert(serviceList.size == 1, "'Start kafka services' should be submitted.")
    val ss = Cluster.k8sClient.apps()
      .statefulSets()
      .createOrReplace(KafkaStatefulSet.statefulSet(clusterConfig))
    logger.info(s"Stateful set: ${ss.getMetadata.getName} submitted.")
    ClusterUtils.waitForClusterUpAndReady(client = Cluster.k8sClient, ss,
      timeoutSeconds = clusterConfig.startTimeoutSeconds)

    val pods = Cluster.k8sClient.pods().withLabels(Services.labels(prefix))
      .list().getItems.asScala

    podsAssigned.appendAll(pods)

    logger.info(
      s"""
         |Kafka cluster started.
         |
         |Pods Names: ${pods.map(_.getMetadata.getName).mkString("\n")}
         |
         |Services: ${serviceList.map(_.getMetadata.getName).mkString("\n")}
         |
         |Published services:
         |$serviceAddresses
       """.stripMargin)
  }

  /**
    * Determine whether the Kafka Cluster is up and running, in the given timeout.
    *
    * @param timeoutSeconds Total timeout.
    * @return Returns false in both cases, one it is determined the cluster is not running or,
    *         it could not be ascertained in the given timeout.
    */
  override def isRunning(timeoutSeconds: Int = 5): Boolean = {
    val serviceList = Cluster.k8sClient.services().list().getItems.asScala

    val isBrokerServiceUp = serviceList.exists(_.getMetadata.getName ==
      Services.brokerService(prefix).getMetadata.getName)

    val ssName = KafkaStatefulSet.statefulSet(clusterConfig).getMetadata.getName
    val ss = Cluster.k8sClient.apps().statefulSets().withName(ssName).get()

    isBrokerServiceUp && ClusterUtils.waitForClusterUpAndReady(Cluster.k8sClient, ss,
       timeoutSeconds, throwException = false)
  }

  override def stop(): Unit = {
    Cluster.k8sClient.services().delete(serviceList.asJava)
    Cluster.k8sClient.apps().statefulSets()
      .delete(KafkaStatefulSet.statefulSet(clusterConfig))
    podsAssigned.foreach(Cluster.k8sClient.pods().delete(_))
  }

  override def getPods: Seq[Pod] = podsAssigned
}

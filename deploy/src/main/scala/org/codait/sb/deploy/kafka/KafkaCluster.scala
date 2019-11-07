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

import io.fabric8.kubernetes.api.model.Pod
import org.codait.sb.deploy.{Address, Cluster, ServiceAddresses}
import org.codait.sb.util.ClusterUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer


class KafkaCluster(override val clusterConfig: KafkaClusterConfig)  extends Cluster {
  private val prefix = clusterConfig.clusterPrefix

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def serviceList =
    Cluster.kubernetesClient.services()
      .withLabels(Services.labels(prefix)).list().getItems.asScala

  private def brokerService() = {
    serviceList.find(_.getMetadata.getName ==
      Services.brokerService(prefix).getMetadata.getName)
  }

  override def serviceAddresses: Array[ServiceAddresses] = {
    assert(isRunning(5), "Kafka service is not running.")
    Array(ServiceAddresses(
      internalAddress =
        Some(Address(Helpers.kafkaServiceName(prefix), Constants.KAFKA_BROKER_PORT)),
      externalAddress =
        Some(Address(
          host = s"${KafkaStatefulSet.statefulSet(clusterConfig).getMetadata.getName}-0" +
            s".${Helpers.kafkaServiceName(prefix)}.${clusterConfig.kubernetesNamespace}" +
            s".svc.cluster.local",
          port = Services.getNodePort(brokerService().get)))))
  }

  private val podsAssigned = ArrayBuffer[Pod]()

  override def start(): Unit = {
    Cluster.kubernetesClient.services().createOrReplace(Services.brokerService(prefix))
    logger.info("Starting kafka services.")

    assert(serviceList.size == 1, "'Start kafka services' should be submitted.")
    val ss = Cluster.kubernetesClient.apps()
      .statefulSets()
      .createOrReplace(KafkaStatefulSet.statefulSet(clusterConfig))
    logger.info(s"Stateful set: ${ss.getMetadata.getName} submitted.")
    ClusterUtils.waitForClusterUpAndReady(client = Cluster.kubernetesClient, ss,
      timeoutSeconds = clusterConfig.startTimeoutSeconds)

    val pods = Cluster.kubernetesClient.pods().withLabels(Services.labels(prefix))
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
         |${serviceAddresses.mkString(",")}
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
    val serviceList = Cluster.kubernetesClient.services().list().getItems.asScala

    val isBrokerServiceUp = serviceList.exists(_.getMetadata.getName ==
      Services.brokerService(prefix).getMetadata.getName)

    val ssName = KafkaStatefulSet.statefulSet(clusterConfig).getMetadata.getName
    val ss = Cluster.kubernetesClient.apps().statefulSets().withName(ssName).get()

    isBrokerServiceUp && ClusterUtils.waitForClusterUpAndReady(Cluster.kubernetesClient, ss,
       timeoutSeconds, throwException = false)
  }

  override def stop(): Unit = {
    Cluster.kubernetesClient.services().delete(serviceList.asJava)
    Cluster.kubernetesClient.apps().statefulSets()
      .delete(KafkaStatefulSet.statefulSet(clusterConfig))
    podsAssigned.foreach(Cluster.kubernetesClient.pods().delete(_))
  }

  override def getPods: Seq[Pod] = podsAssigned
}

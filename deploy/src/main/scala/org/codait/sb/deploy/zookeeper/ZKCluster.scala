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

package org.codait.sb.deploy.zookeeper

import io.fabric8.kubernetes.api.model.Pod
import org.codait.sb.deploy.Cluster
import org.codait.sb.util.ClusterUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer


class ZKCluster(override val clusterConfig: ZKClusterConfig) extends Cluster {

  private val prefix = clusterConfig.clusterPrefix

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  private def serviceList =
    Cluster.kubernetesClient.services().withLabels(Services.labels(prefix)).list().getItems.asScala

  override def serviceAddresses: Map[String, String] = Map("zookeeper" ->
    s"${Helpers.zkClientServiceName(prefix)}:${Constants.ZK_CLIENT_PORT}")

  private val podsAssigned = ArrayBuffer[Pod]()

  override def start(): Unit = {
    Cluster.kubernetesClient.services().createOrReplace(Services.clientService(prefix))
    Cluster.kubernetesClient.services().createOrReplace(Services.internalService(prefix))
    logger.info("Starting zookeeper services.")

    assert(serviceList.size == 2, "'Start Zookeeper services' should be submitted.")
    val ss = Cluster.kubernetesClient.apps()
      .statefulSets()
      .createOrReplace(ZKStatefulSet.statefulSet(clusterConfig))
    ClusterUtils.waitForClusterUpAndReady(client = Cluster.kubernetesClient, ss,
      timeoutSeconds = clusterConfig.startTimeoutSeconds)

    val pods = Cluster.kubernetesClient.pods().withLabels(Services.labels(prefix))
      .list().getItems.asScala

    podsAssigned.appendAll(pods)
    logger.info(
      s"""
         |Zookeeper cluster started.
         |
         |Pods Names: ${pods.map(_.getMetadata.getName).mkString("\n")}
         |
         |Services: ${serviceList.map(_.getMetadata.getName).mkString("\n")}
         |
         |Zookeeper service: $serviceAddresses
       """.stripMargin)
  }

  /**
    * Determine whether the Zookeeper Cluster is up and running, in the given timeout.
    *
    * @param timeoutSeconds Total timeout.
    * @return Returns false in both cases, one it is determined the cluster is not running or,
    *         it could not be ascertained in the given timeout.
    */
  override def isRunning(timeoutSeconds: Int = 5): Boolean = {
    val serviceList = Cluster.kubernetesClient.services().list().getItems.asScala

    val isClientServiceUp =
      serviceList
        .exists(_.getMetadata.getName == Services.clientService(prefix).getMetadata.getName)

    val isInternalServiceUp =
      serviceList
        .exists(_.getMetadata.getName == Services.internalService(prefix).getMetadata.getName)

    val ssName = ZKStatefulSet
      .statefulSet(clusterConfig).getMetadata.getName
    def ss = Cluster.kubernetesClient.apps().statefulSets().withName(ssName).get()

    isClientServiceUp && isInternalServiceUp &&
      ClusterUtils.waitForClusterUpAndReady(
        Cluster.kubernetesClient, ss, timeoutSeconds, false)
  }

  override def stop(): Unit = {
    Cluster.kubernetesClient.services().delete(serviceList.asJava)
    Cluster.kubernetesClient.apps().statefulSets()
      .delete(ZKStatefulSet.statefulSet(clusterConfig))
    podsAssigned.foreach(Cluster.kubernetesClient.pods().delete(_))
  }

  // TODO: make a fresh query to kubernetes, everytime.
  override def getPods: Seq[Pod] = podsAssigned
}

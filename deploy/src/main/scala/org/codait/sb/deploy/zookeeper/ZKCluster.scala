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

import scala.collection.JavaConverters._
import org.codait.sb.deploy.{Cluster, ClusterConfig}
import org.codait.sb.util.ClusterUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer


class ZKCluster(override val clusterConfig: ZKClusterConfig) extends Cluster {

  private val prefix = clusterConfig.clusterPrefix

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  private def serviceList =
    kubernetesClient.services().withLabels(Services.labels(prefix)).list().getItems.asScala

  val serviceAddress: String = s"${Helpers.zkClientServiceName(prefix)}:${Constants.ZK_CLIENT_PORT}"

  private val podsAssigned = ArrayBuffer[Pod]()

  def start(): Unit = {
    kubernetesClient.services().createOrReplace(Services.clientService(prefix))
    kubernetesClient.services().createOrReplace(Services.internalService(prefix))
    logger.info("Starting zookeeper services.")

    assert(serviceList.size == 2, "'Start Zookeeper services' should be submitted.")
    val ss = kubernetesClient.apps()
      .statefulSets()
      .createOrReplace(ZKStatefulSet.statefulSet(prefix, clusterConfig.replicaSize))
    ClusterUtils.waitForClusterUpAndReady(client = kubernetesClient, ss, timeoutSeconds = 20)

    val pods = kubernetesClient.pods().withLabels(Services.labels(prefix))
      .list().getItems.asScala

    podsAssigned.appendAll(pods)
    logger.info(
      s"""
         |Zookeeper cluster started.
         |
         |Pods Names: ${pods.map(_.getMetadata.getName).mkString("\n")}
         |
         |Services: ${serviceList.map(_.getMetadata.getName).mkString("\n")}
       """.stripMargin)
  }

  /**
    * Determine whether the Zookeeper Cluster is up and running, in the given timeout.
    *
    * @param timeoutSeconds Total timeout.
    * @return Returns false in both cases, one it is determined the cluster is not running or,
    *         it could not be ascertained in the given timeout.
    */
  def isRunning(timeoutSeconds: Int = 5): Boolean = {
    val serviceList = kubernetesClient.services().list().getItems.asScala

    val isClientServiceUp =
      serviceList
        .exists(_.getMetadata.getName == Services.clientService(prefix).getMetadata.getName)

    val isInternalServiceUp =
      serviceList
        .exists(_.getMetadata.getName == Services.internalService(prefix).getMetadata.getName)

    val ssName = ZKStatefulSet
      .statefulSet(prefix, clusterConfig.replicaSize).getMetadata.getName
    def ss = kubernetesClient.apps().statefulSets().withName(ssName).get()

    isClientServiceUp && isInternalServiceUp &&
      ClusterUtils.waitForClusterUpAndReady(
        kubernetesClient, ss, timeoutSeconds, false)
  }

  def stop(): Unit = {
    kubernetesClient.services().delete(serviceList.asJava)
    kubernetesClient.apps().statefulSets()
      .delete(ZKStatefulSet.statefulSet(prefix, clusterConfig.replicaSize))
  }

  override def getPods: Seq[Pod] = podsAssigned // TODO: make a fresh query to kubernetes, everytime.
}

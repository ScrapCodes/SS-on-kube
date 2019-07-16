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

package org.codait.sb.deploy.spark

import io.fabric8.kubernetes.api.model.Pod
import org.codait.sb.deploy.Cluster
import org.codait.sb.util.{ClusterUtils, DeploymentException}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

class SparkJobClusterDeployViaPod (override val clusterConfig: SparkJobClusterConfig) extends Cluster {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  private def getPodPhase(podName: String): String =
    Cluster.kubernetesClient.pods().withName(podName).get().getStatus.getPhase

  // https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-phase
  private def isPodCompleted(podName: String) = {
    getPodPhase(podName).equalsIgnoreCase("Failed") ||
      getPodPhase(podName).equalsIgnoreCase("Succeeded")
  }

  private var started: Boolean = false

  private val sparkDriverDeploy = new SparkDriverDeploy(clusterConfig)
  private var sparkDriverDeployPod: Option[Pod] = None

  def start(): Unit = synchronized {
    if (started) {
      throw new DeploymentException("Already started")
    }
    started = true
    sparkDriverDeployPod = Some(sparkDriverDeploy.deployViaPod(Cluster.kubernetesClient))
    logger.info(s"Deployed spark submit pod ${sparkDriverDeployPod.get.getMetadata.getName}")
  }

  def waitUntilSparkDriverCompletes(timeoutSeconds: Int): Boolean = {
    def podName = if (clusterConfig.sparkDeployMode.equalsIgnoreCase("client")) {
      sparkDriverDeployPod.map(_.getMetadata.getName)
    } else {
      // In this case the driver is submitted via a Pod, in other words, driver pod name
      // is different from this deployer(submitting) pod. So to get it we need to parse Log
      // output of this node.
      getPods.find(_.getMetadata.getName.contains("driver")).map(_.getMetadata.getName)
    }

    ClusterUtils.reAttempt(timeoutSeconds = timeoutSeconds,
      condition = () => podName.isDefined && isPodCompleted(podName.get),
      msg = () => s"Spark driver pod with name: $podName, did not complete in time." +
        s" Current phase: ${getPodPhase(podName.get)}")
  }

  override def serviceAddresses: Map[String, String] = Map()

  override def getPods: Seq[Pod] = {
    Cluster.kubernetesClient.pods().list().getItems.asScala
      .filter(_.getMetadata.getName.contains(clusterConfig.name))
  }

  override def stop(): Unit = {
    val pods = getPods
    pods.foreach(Cluster.kubernetesClient.pods().delete(_))
    Cluster.kubernetesClient.services()
      .withName(sparkDriverDeploy.sparkDriverService.getMetadata.getName).delete()
    Cluster.kubernetesClient.services().list().getItems.asScala
      .filter(_.getMetadata.getName.contains(clusterConfig.name))
      .foreach(Cluster.kubernetesClient.services().delete(_))
  }

  override def isRunning(timeoutSeconds: Int): Boolean = {
    def driverPod: Option[Pod] = if (clusterConfig.sparkDeployMode.equalsIgnoreCase("cluster")) {
      getPods.find(_.getMetadata.getName.contains("driver"))
    } else {
      getPods.find(_.getMetadata.getName.contains("deploy"))
    }
    if (driverPod.exists(x => isPodCompleted(x.getMetadata.getName))) {
      false
    } else {
      logger.info(s"Pod might be in pending, probing. ${driverPod.map(_.getStatus.getPhase)}")
      // If the pod is in pending state, then we wait for timeout to see if it
      // transitions to running.
      ClusterUtils.reAttempt(timeoutSeconds = timeoutSeconds,
        condition =
          () => driverPod.exists(_.getStatus.getPhase.equalsIgnoreCase("Running")),
        throwException = false
      )
    }

  }

}
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

import java.io.File

import io.fabric8.kubernetes.api.model.Pod
import org.codait.sb.deploy.Cluster
import org.codait.sb.util.{ClusterUtils, DeploymentException}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.io.Source


class SparkJobCluster(override val clusterConfig: SparkJobClusterConfig) extends Cluster {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  private def getPodPhase(podName: String): String =
    Cluster.k8sClient.pods().withName(podName).get().getStatus.getPhase

  private def logFile(kind: String): File = {
    val tmpDir = System.getProperty("java.io.tmpdir")
    assert(new File(tmpDir).exists(), "property java.io.tmpdir points to an inexistent dir.")
    new File(tmpDir + s"/spark_${clusterConfig.name}_$kind.log")
  }

  private val errorLog: File = logFile("error")
  private val outputLog: File = logFile("output")
  // https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-phase
  private def isPodCompleted(podName: String) = {
    getPodPhase(podName).equalsIgnoreCase("Failed") ||
      getPodPhase(podName).equalsIgnoreCase("Succeeded")
  }

  private var sparkProcess: Option[Process] = None
  private var started: Boolean = false

  private val sparkDriverDeploy = new SparkDriverDeploy(clusterConfig)

  def start(): Unit = synchronized {
    if (started) {
      throw new DeploymentException("Already started")
    }
    started = true
    sparkProcess = Some(sparkDriverDeploy.deployLocally(errorLog, outputLog))
  }

  def waitUntilSparkDriverCompletes(timeoutSeconds: Int): Boolean = {
    val podName = if (clusterConfig.sparkDeployMode.equalsIgnoreCase("cluster")) {
      ClusterUtils.reAttempt(
        condition = () => parsePodNameFromLogs(errorLog).isDefined,
        timeoutSeconds = timeoutSeconds,
        msg = () => s"Unable to parse $errorLog, to retrieve pod name for driver.")

      val podName = parsePodNameFromLogs(errorLog).get

      logger.info(s"Spark driver pod name: $podName")
      podName
    } else {
      throw new DeploymentException("Deploy locally with client mode is not supported.")
    }
    ClusterUtils.reAttempt(timeoutSeconds = timeoutSeconds,
      condition = () => isPodCompleted(podName),
      msg = () => s"Spark driver pod with name: $podName, did not complete in time." +
        s" Current phase: ${getPodPhase(podName)}")
  }

  private def parsePodNameFromLogs(path: File): Option[String] = {
    val logFile = path
    assert(logFile.exists(), s"$logFile does not exists, did spark job ran?")
    val bufferedSource = Source.fromFile(path)
    val strings = bufferedSource.getLines()
    val line: Option[String] = strings.find(_.contains("pod name:"))
    bufferedSource.close()
    line.map { l =>
      val (_, podName) = l.splitAt(l.indexOf(':') + 1)
      podName.trim
    }
  }

  override def serviceAddresses: Map[String, String] = Map()

  override def getPods: Seq[Pod] = {
    if (clusterConfig.sparkDeployMode.equalsIgnoreCase("cluster")) {
      Cluster.k8sClient.pods().list().getItems.asScala
        .filter(_.getMetadata.getName.contains(clusterConfig.name))
    } else {
      throw new DeploymentException("Not supported.")
    }
  }

  override def stop(): Unit = {
    if(clusterConfig.sparkDeployMode.equalsIgnoreCase("cluster")) {
      val pods = getPods
      pods.foreach(Cluster.k8sClient.pods().delete(_))
    } else {
      if (sparkProcess.isDefined) {
        sparkProcess.get.destroy()
      } else {
        logger.warn("Spark cluster is not started.")
      }
    }
  }

  override def isRunning(timeoutSeconds: Int): Boolean = {
    def driverPod = getPods.filter(_.getMetadata.getName.contains("driver"))
    if (clusterConfig.sparkDeployMode.equalsIgnoreCase("cluster")) {
      if (driverPod.exists(x => isPodCompleted(x.getMetadata.getName))) {
        false
      } else {
        // If the pod is in pending state, then we wait for timeout to see if it
        // transitions to running.
        ClusterUtils.reAttempt(timeoutSeconds = timeoutSeconds,
          condition =
            () => driverPod.exists(_.getStatus.getPhase.equalsIgnoreCase("Running")),
          throwException = false
        )
      }
    } else {
      if (started && sparkProcess.isDefined && sparkProcess.get.isAlive) {
        val c = () => {
          val bufferedSource = Source.fromFile(outputLog)
          val hasJobProducedOutput = bufferedSource.mkString.length > 1
          bufferedSource.close()
          hasJobProducedOutput
        }
        ClusterUtils.reAttempt(timeoutSeconds = timeoutSeconds,
          condition = c,
          throwException = false)
      } else {
        false
      }
    }
  }

  def fetchOutputLog(): String = {
    val bufferedSource = Source.fromFile(outputLog)
    val content = bufferedSource.mkString("\n")
    bufferedSource.close()
    content
  }

  def fetchErrorLog(): String = {
    val bufferedSource = Source.fromFile(errorLog)
    val content = bufferedSource.mkString("\n")
    bufferedSource.close()
    content
  }
}

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

import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.codait.sb.util.{SBConfig, SanityTestUtils}
import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source

private[deploy]
object SparkCluster {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))
  private lazy val kubernetesClient = new DefaultKubernetesClient()
    .inNamespace(SBConfig.NAMESPACE)

  def masterURL: String = "k8s://" + SBConfig.masterUrl

  def start(name: String,
            className: String,
            sparkImage: String,
            kubeServiceAccount: String,
            pathToJar: String,
            timeoutSeconds: Int): Unit = {
    assert(SBConfig.masterUrl != null, "Please specify kubernetes master url, using sb.kubernetes.master")

    val sparkHome: Option[String] =
      Option(System.getenv("SPARK_HOME")).orElse(Option(System.getProperty("sb.spark.home")))

    val kubeConfig: Option[String] = Option(System.getenv("KUBECONFIG"))

    assert(sparkHome.isDefined,
      "Please export spark home as SPARK_HOME or specify the sb.spark.home configuration.")

    assert(kubeConfig.isDefined, "Please export path to kubernetes config as kubeConfig env variable.")

    val sparkSubmitPath = sparkHome.last + "/bin/spark-submit"

    // In case a user has set a wrong path to SPARK_HOME.
    assert(new File(sparkSubmitPath).exists(),
      s"Please specify the correct value for SPARK_HOME, $sparkSubmitPath path not found.")
    val errorLog = new File(System.getProperty("java.io.tmpdir") + s"/spark_${name}_error.log")
    val outputLog = new File(System.getProperty("java.io.tmpdir") + s"/spark_${name}_output.log")
    val sparkSubmitCommand = Seq(sparkSubmitPath,
      "--master", masterURL,
      "--deploy-mode", "cluster",
      "--name", name,
      "--class", className,
      "--conf", "spark.executor.instances=5", //TODO make it configurable
      "--conf", s"spark.kubernetes.container.image=$sparkImage",
      "--conf", s"spark.kubernetes.authenticate.driver.serviceAccountName=$kubeServiceAccount",
      pathToJar)
    val sparkProcessBuilder = new ProcessBuilder().command(sparkSubmitCommand :_*)
      .redirectOutput(outputLog)
      .redirectError(errorLog)
      .directory(new File(sparkHome.get))
    logger.info(s"Starting, spark job with command: ${sparkSubmitCommand.mkString(" ")}")
    sparkProcessBuilder.start()
    waitUntilSparkDriverCompletes(outputLog.getAbsolutePath, timeoutSeconds)
    logger.info("Spark job finished.")
  }

  private def waitUntilSparkDriverCompletes(logPath: String, timeoutSeconds: Int): Boolean = {
    SanityTestUtils.reAttempt(condition = ()  => parsePodNameFromLogs(logPath).isDefined,
      timeoutSeconds = timeoutSeconds)
    val podName = parsePodNameFromLogs(logPath).get

    logger.info(s"Spark driver pod name: $podName")

    def getPodPhase: String = kubernetesClient.pods().withName(podName).get().getStatus.getPhase

    SanityTestUtils.reAttempt( timeoutSeconds = timeoutSeconds,
      condition = () => getPodPhase.equalsIgnoreCase("completed") ||
        getPodPhase.equalsIgnoreCase("Succeeded"),
      msg = () => s"Spark driver pod with name: $podName, did not complete in time." +
        s" Current phase: $getPodPhase")
  }

  private def parsePodNameFromLogs(path: String): Option[String] = {
    val logFile = new File(path)
    assert(logFile.exists(), s"$logFile does not exists, did spark job ran?")
    val bufferedSource = Source.fromFile(path)
    val line: Option[String] = bufferedSource.getLines().find(_.contains("pod name:"))
    bufferedSource.close()
    line.map { l =>
      val (_, podName) = l.splitAt(l.indexOf(':') + 1)
      podName.trim
    }
  }

}

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

import io.fabric8.kubernetes.api.model._
import io.fabric8.kubernetes.client.KubernetesClient
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

class SparkDriverDeploy(clusterConfig: SparkJobClusterConfig) {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))
  private val driverPodName = s"spark-${clusterConfig.name}-deploy"
  private val sparkDriverServiceName = s"$driverPodName-svc"
  private val driverPortName = "driver-port"
  private val blockManagerPortName = "blkmgr-port"

  private val labels = Map("app" -> s"spark-${clusterConfig.name}").asJava

  def sparkSubmitCommand(containerMode: Boolean): Seq[String] = {
    assert(clusterConfig.masterUrl != null, "Master url needs to be specified.")

    val sparkHome: Option[String] = clusterConfig.sparkHome

    val kubeConfig: Option[String] =
      Option(System.getenv("KUBECONFIG"))

    assert(kubeConfig.isDefined,
      "Please export path to kubernetes config as KUBECONFIG env variable.")
    val sparkSubmitPath = if (containerMode) {
      "/opt/spark/bin/spark-submit"
    } else {
      // In case a user has set a wrong path to SPARK_HOME.
      val sparkSubmitPath = sparkHome.get + "/bin/spark-submit"
      assert(new File(sparkSubmitPath).exists(),
        s"Please specify the correct value for spark home, $sparkSubmitPath path not found.")
      sparkSubmitPath
    }
    val packages: Seq[String] = if (clusterConfig.packages.nonEmpty) {
      Seq("--packages", clusterConfig.packages.mkString("", ",", ""))
    } else {
      Seq()
    }
    val extraConfigClientMode =
      if (clusterConfig.sparkDeployMode.equalsIgnoreCase("client")) {
        Seq("--conf", s"spark.driver.port=${clusterConfig.sparkDriverPort}",
          "--conf", s"spark.driver.blockManager.port=${clusterConfig.sparkBlockManagerPort}",
          "--conf", s"spark.kubernetes.driver.pod.name=$driverPodName",
         "--conf", s"spark.driver.host=" +
            s"$sparkDriverServiceName.${clusterConfig.kubernetesNamespace}.svc")
      } else {
        Seq()
      }
    val sparkSubmitCommand = Seq(sparkSubmitPath,
      "--master", clusterConfig.masterUrl,
      "--deploy-mode", clusterConfig.sparkDeployMode,
      "--name", clusterConfig.name,
      "--class", clusterConfig.className,
      "--conf", "spark.kubernetes.container.image.pullPolicy=Always",
      "--conf", s"spark.kubernetes.namespace=${clusterConfig.kubernetesNamespace}",
      "--conf", s"spark.executor.instances=${clusterConfig.numberOfExecutors}",
      "--conf", s"spark.kubernetes.container.image=${clusterConfig.sparkImage}",
      "--conf", "spark.kubernetes.authenticate.oauthTokenFile=" +
        "/var/run/secrets/kubernetes.io/serviceaccount/token",
      "--conf", "spark.kubernetes.authenticate.caCertFile=" +
        "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt",
      "--conf",
      s"spark.kubernetes.authenticate.driver.serviceAccountName=${clusterConfig.serviceAccount}") ++
      packages ++ extraConfigClientMode ++
      clusterConfig.configParams.flatMap { x =>
        Seq("--conf", s"${x._1}=${x._2}")
      } ++ Seq(clusterConfig.pathToJar) ++ clusterConfig.commandArgs

    sparkSubmitCommand
  }

  // Needs spark distribution configured as spark home.
  def deployLocally(errorLog: File, outputLog: File): Process = {
    val sparkCommand = sparkSubmitCommand(containerMode = false)
    errorLog.delete()
    outputLog.delete()
    logger.info(s"Spark job directs STDERR and STDOUT to $errorLog and $outputLog respectively.")
    val sparkProcessBuilder: ProcessBuilder = new ProcessBuilder().command(sparkCommand: _*)
      .redirectOutput(outputLog)
      .redirectError(errorLog)
      .directory(new File(clusterConfig.sparkHome.get))
    logger.info(s"Starting, spark job with command: ${sparkCommand.mkString(" ")}")
    val process: Process = sparkProcessBuilder.start()
    logger.info("Spark job submitted.")
    process
  }

  def deployViaPod(client: KubernetesClient): Pod = {
    val pod = sparkDriverPod()
    logger.info(s"Submitting spark driver deploy pod ${pod.getMetadata.getName}, " +
      s"with command: ${pod.getSpec.getContainers.get(0).getCommand.asScala.mkString('\n'.toString)}")
    if (clusterConfig.sparkDeployMode.equalsIgnoreCase("client")) {
      client.services().create(sparkDriverService)
    }
    client.pods().createOrReplace(pod)
  }

  def sparkDriverPod(): Pod = {
    new PodBuilder()
      .withApiVersion("v1")
      .withKind("Pod")
      .editOrNewMetadata()
        .withName(driverPodName)
        .withLabels(labels)
        .endMetadata()
      .withNewSpec()
      .withRestartPolicy("Never")
      .withServiceAccountName(clusterConfig.serviceAccount)
      .withServiceAccount(clusterConfig.serviceAccount)
      .addNewContainer()
        .withName(s"${clusterConfig.name}-container")
        .withImage(clusterConfig.sparkImage)
        .withImagePullPolicy("Always")
        .withCommand(sparkSubmitCommand(containerMode = true).asJava)
        .addNewPort()
          .withName(driverPortName)
          .withContainerPort(clusterConfig.sparkDriverPort)
          .withProtocol("TCP")
          .endPort()
        .addNewPort()
          .withName(blockManagerPortName)
          .withContainerPort(clusterConfig.sparkBlockManagerPort)
          .withProtocol("TCP")
          .endPort()
        .endContainer()
      .endSpec()
      .build()
  }

  def sparkDriverService: Service = {
    new ServiceBuilder()
      .withApiVersion("v1")
      .withKind("Service")
      .editOrNewMetadata()
        .withName(sparkDriverServiceName)
        .withLabels(labels)
        .endMetadata()
      .withNewSpec()
      .withClusterIP("None")
      .withSelector(labels)
        .addNewPort()
          .withName(driverPortName)
          .withPort(clusterConfig.sparkDriverPort)
          .withNewTargetPort(clusterConfig.sparkDriverPort)
          .endPort()
        .addNewPort()
          .withName(blockManagerPortName)
          .withPort(clusterConfig.sparkBlockManagerPort)
          .withNewTargetPort(clusterConfig.sparkBlockManagerPort)
          .endPort()
      .endSpec()
      .build()
  }
}
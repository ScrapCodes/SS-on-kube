/*
 *
 * Streaming Benchmark
 *
 *
 *
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package org.codait.sb.demo.deploy

import java.util.UUID

import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.codait.sb.deploy.Address
import org.codait.sb.deploy.kafka.{KafkaCluster, KafkaClusterConfig}
import org.codait.sb.deploy.microservice.{MicroServiceCluster, MicroServiceClusterConfig}
import org.codait.sb.deploy.spark.{SparkJobClusterConfig, SparkJobClusterDeployViaPod}
import org.codait.sb.deploy.zookeeper.{ZKCluster, ZKClusterConfig}

/**
  * Entire demo is orchestrated here. Spark jobs are already deployed to docker image and
  * are available on scrapcodes/spark:v2.4.4-sb. Use the script `build_and_push.sh`, to
  * deploy the image to docker hub repo of your choice. The image needs to be updated in case
  * there is an update to spark job.
  */
object KubeDeployDemo {

  private val serviceAccount = "spark"
  private val k8sNameSpace = "default"
  private val clusterPrefix = s"demo-${UUID.randomUUID().toString.take(4)}"
  private val sparkVersion: String = "2.4.4"
  private val sparkImage = s"scrapcodes/spark:v$sparkVersion-sb-1"

  def main(args: Array[String]): Unit = {
    /*
     We need following components in this demo, namely:
     a) zookeeper, b) kafka, c) spark, d) k8s cluster
     In order to run this demo, a kubernetes cluster should be available, with
     service account spark created on it. Use the following commands to create it
      using kubectl
     1) kubectl create serviceaccount spark
     2) kubectl create clusterrolebinding spark-role --clusterrole=edit \
     --serviceaccount=default:spark --namespace=default
    */

    // Support single node cluster e.g. minikube.
      val client = new DefaultKubernetesClient()
    val replicaCounts = if( client.nodes().list().getItems.size() < 3) {
        1
      } else {
        3
      }

    // Create Microservice serving the ML model using MAX on Kubernetes.
    val microServiceClusterConfig = MicroServiceClusterConfig(
      clusterPrefix = clusterPrefix,
      clusterName = "text-senti-classify",
      initialReplicaSize = 1,
      microServiceImage = "codait/max-text-sentiment-classifier",
      namedServicePorts = Map("rest" -> 5000),
      serviceAccount = "spark"
    )
    val maxModelEvaluatorRestService = new MicroServiceCluster(microServiceClusterConfig)
    maxModelEvaluatorRestService.start()

    // Start a 3 node zookeeper service for kafka.
    val zkClusterConfig = ZKClusterConfig(clusterPrefix,
      replicaCounts, startTimeoutSeconds = 300, k8sNameSpace, serviceAccount)
    val zkCluster = new ZKCluster(zkClusterConfig)
    zkCluster.start()
    assert(zkCluster.isRunning(360))

    // Start the Kafka service once zookeeper is up and running.
    val kafkaTopic: String = "tweets"
    val zkAddress: Address = zkCluster.serviceAddresses.head.internalAddress.get
    val kafkaClusterConfig =
      KafkaClusterConfig(clusterPrefix,
        replicaCounts, zkAddress, startTimeoutSeconds = 300, k8sNameSpace, serviceAccount)
    val kafkaCluster = new KafkaCluster(kafkaClusterConfig)
    kafkaCluster.start()
    assert(kafkaCluster.isRunning(360))
    assert(maxModelEvaluatorRestService.isRunning(360))

    // Once Kafka service is up and our ML model is serving the requests, we are
    // ready to start our Spark structured streaming jobs for performing analytics.

    val kafkaBrokerAddress =
      kafkaCluster.serviceAddresses.head.internalAddress.get.toString

    val sparkClusterDataGeneratorConfig =
      SparkJobClusterConfig(name = "spark-data-generator",
        masterUrl = s"k8s://https://kubernetes.$k8sNameSpace.svc",
        sparkDeployMode = "client",
        className = "org.codait.sb.demo.SparkStreamingDataGenerator",
        sparkImage = sparkImage,
        pathToJar = s"local:///opt/jars/demo_2.11-0.1.0-SNAPSHOT.jar",
        numberOfExecutors = 1,
        configParams = Map("spark.jars.ivy" -> "/tmp/.ivy"),
        sparkHome = None,
        packages = Seq(s"org.apache.spark:spark-sql-kafka-0-10_2.11:$sparkVersion"),
        commandArgs = Seq(kafkaBrokerAddress, kafkaTopic),
        kubernetesNamespace = k8sNameSpace,
        serviceAccount = serviceAccount)

    val hostPort =
      maxModelEvaluatorRestService.serviceAddresses.head.internalAddress.get.toString

    val restAddress = s"http://$hostPort/model/predict"
    val sparkClusterMLPipelineConfig =
      SparkJobClusterConfig(name = "spark-ml-pipeline",
        masterUrl = s"k8s://https://kubernetes.$k8sNameSpace.svc",
        sparkDeployMode = "client",
        className = "org.codait.sb.demo.SparkStreamingMLPipeline",
        sparkImage = sparkImage,
        pathToJar = s"local:///opt/jars/demo_2.11-0.1.0-SNAPSHOT.jar",
        numberOfExecutors = 1,
        configParams = Map(("spark.jars.ivy" -> "/tmp/.ivy"),
          // Job output is lost in logging statements, so turning off.
          ("spark.driver.extraJavaOptions" ->
          "-Dlog4j.configuration=file:///opt/jars/error-only-log4j.properties")),
        sparkHome = None,
        packages = Seq(s"org.apache.spark:spark-sql-kafka-0-10_2.11:$sparkVersion",
          "com.googlecode.json-simple:json-simple:1.1"),
        commandArgs = Seq(kafkaBrokerAddress, kafkaTopic, restAddress),
        kubernetesNamespace = k8sNameSpace,
        serviceAccount = serviceAccount)

    val sparkJobClusterDataGenerator = new SparkJobClusterDeployViaPod(sparkClusterDataGeneratorConfig)
    sparkJobClusterDataGenerator.start()

    val sparkJobClusterMLPipeline = new SparkJobClusterDeployViaPod(sparkClusterMLPipelineConfig)
    sparkJobClusterMLPipeline.start()
  }
}

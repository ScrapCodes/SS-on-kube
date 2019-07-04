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

package org.codait.sb.it.spark

import org.codait.sb.deploy.kafka.{KafkaCluster, KafkaClusterConfig}
import org.codait.sb.deploy.spark.{SparkJobClusterConfig, SparkJobClusterDeployViaPod}
import org.codait.sb.deploy.zookeeper.{ZKCluster, ZKClusterConfig}
import org.codait.sb.util.ClusterUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually.{eventually, interval, timeout}

import scala.concurrent.duration._

class SparkClusterModeSuite extends SparkSuiteBase with BeforeAndAfterAll {
  private var zkCluster: ZKCluster = _
  private var kafkaCluster: KafkaCluster = _

  private def startKafkaIfRequired(): (String, String) = {
    if (existingKafkaPodName.nonEmpty && existingKafkaService.nonEmpty) {
      (existingKafkaService, existingKafkaPodName)
    } else {
      // This test needs three clusters running in order.
      // 1. Zookeeper
      zkCluster = new ZKCluster(ZKClusterConfig("z" + testingPrefix, 3, testK8sNamespace))
      zkCluster.start()
      assert(zkCluster.isRunning(10), "Zookeeper should be up and running.")
      val zkAddress = zkCluster.serviceAddresses("zookeeper")
      //2. Kafka Cluster
      kafkaCluster = new KafkaCluster(
        KafkaClusterConfig("k" + testingPrefix, 3,
          zkAddress, startTimeoutSeconds = 120, testK8sNamespace))

      kafkaCluster.start()
      assert(kafkaCluster.isRunning(10), "Kafka should be up and running.")
      val brokerAddress = kafkaCluster.serviceAddresses("kafka-broker-internal")
      val kafkaPodName = kafkaCluster.getPods.head.getMetadata.getName
      (brokerAddress, kafkaPodName)
    }
  }

  private lazy val (brokerAddress, kafkaPodName) = startKafkaIfRequired()

  test("Spark streaming kafka.") {
    // This test needs three clusters running in order.
    val topic = s"t$testingPrefix"

    val conf = SparkJobClusterConfig("s2" + testingPrefix,
      s"k8s://https://kubernetes.$testK8sNamespace.svc",
      sparkDeployMode = "client",
      "org.apache.spark.examples.sql.streaming.StructuredKafkaWordCount",
      sparkImagePath,
      serviceAccount,
      pathToJar = examplesJar,
      numberOfExecutors = 2,
      Map(),
      packages = Seq("org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.0"),
      commandArgs = Seq(brokerAddress, "subscribe", topic),
      kubernetesNamespace = testK8sNamespace)
    val sparkJobCluster = new SparkJobClusterDeployViaPod(conf)
    sparkJobCluster.start()
    // Since it is a streaming job, the cluster will keep running till we terminate it.
    assert(sparkJobCluster.isRunning(60),
      s"spark cluster did not start.")

    logger.info(s"spark cluster started. ${sparkJobCluster.getPods.map(_.getMetadata.getName)}")

    eventually(timeout(3.minutes), interval(20.seconds)) {
      val command = s"echo 'test-$topic' | kafka-console-producer.sh --topic $topic --broker-list $brokerAddress"
      val (r, s) = ClusterUtils.execCommand(kafkaCluster.getPods.head, command, k8sClient)
      assert(s, s"Command $command should execute successfully: $r")
      // The deployer pod becomes the driver in client mode.
      val driverPod = sparkJobCluster.getPods.find(_.getMetadata.getName.contains("deploy")).get

      val fetchedDriverLog = k8sClient.pods().withName(driverPod.getMetadata.getName).getLog

      assert(fetchedDriverLog.contains(s"test-$topic"), "Should contain the result.")
    }

    // We close spark cluster only on successful completion of test, allows for debugging the failure.
    sparkJobCluster.stop()
  }

  override def afterAll(): Unit = {
    if (zkCluster != null && kafkaCluster != null) {
      zkCluster.stop()
      kafkaCluster.stop()
    }
  }

}

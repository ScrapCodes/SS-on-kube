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
import org.codait.sb.deploy.spark.{SparkJobCluster, SparkJobClusterConfig}
import org.codait.sb.deploy.zookeeper.{ZKCluster, ZKClusterConfig}
import org.scalatest.BeforeAndAfterAll

class SparkClusterModeSuite extends SparkSuiteBase with BeforeAndAfterAll {
  ignore("Spark streaming kafka.") {
    // This test needs three clusters running in order.
    // 1. Zookeeper
    val zkCluster = new ZKCluster(ZKClusterConfig("z" + testingPrefix, 3, testK8sNamespace))
    zkCluster.start()
    assert(zkCluster.isRunning(10), "Zookeeper should be up and running.")
    val zkAddress = zkCluster.serviceAddresses("zookeeper")
    //2. Kafka Cluster
    val kafkaCluster = new KafkaCluster(
      KafkaClusterConfig("k" + testingPrefix, 3,
        zkAddress, startTimeoutSeconds = 120, testK8sNamespace))

    kafkaCluster.start()
    assert(kafkaCluster.isRunning(10), "Kafka should be up and running.")
    val brokerAddress = kafkaCluster.serviceAddresses("kafka-broker-external")

    val kafkaPodName = kafkaCluster.getPods.head.getMetadata.getName
    val topic = s"t$testingPrefix"

    val conf = SparkJobClusterConfig("s2" + testingPrefix,
      "k8s://https://9.30.110.150:6443",
      deployMode = "client",
      "org.apache.spark.examples.sql.streaming.StructuredKafkaWordCount",
      sparkImagePath,
      serviceAccount,
      pathToJar = localExamplesJar,
      numberOfExecutors = 2,
      Map(),
      sparkHome = sparkHome,
      packages = Seq("org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.0"),
      commandArgs = Seq(brokerAddress, "subscribe", topic),
      testK8sNamespace)
    val sparkJobCluster = new SparkJobCluster(conf)
    sparkJobCluster.start()

    assert(sparkJobCluster.isRunning(120),
      s"spark cluster did not start.")

    k8sClient.pods().withName(kafkaPodName).writingOutput(System.out).writingError(System.err)
      .exec("bash", "-c",
        s"echo 'test-$topic' | kafka-console-producer.sh --topic $topic --broker-list $brokerAddress")


    assert(
      sparkJobCluster.fetchOutputLog().contains(s"test-$topic"),
      "Should contain the result.")
    sparkJobCluster.stop()
    kafkaCluster.stop()
    zkCluster.stop()
  }
}

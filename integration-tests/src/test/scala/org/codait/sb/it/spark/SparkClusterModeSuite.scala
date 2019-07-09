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

import org.codait.sb.deploy.spark.{SparkJobClusterConfig, SparkJobClusterDeployViaPod}
import org.codait.sb.util.ClusterUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually.{eventually, interval, timeout}

import scala.concurrent.duration._

class SparkClusterModeSuite extends SparkSuiteBase with BeforeAndAfterAll {

  import org.codait.sb.it.TestSetup._

  private val brokerAddress = getKafkaCluster.serviceAddresses("kafka-broker-internal")

  test("Spark streaming kafka.") {
    // This test needs three clusters running in order.
    val topic = s"spark$testingPrefix"

    val conf = SparkJobClusterConfig("s2" + testingPrefix,
      s"k8s://https://kubernetes.$testK8sNamespace.svc",
      sparkDeployMode = "client",
      "org.apache.spark.examples.sql.streaming.StructuredKafkaWordCount",
      sparkImagePath,
      pathToJar = examplesJar,
      numberOfExecutors = 2,
      Map(),
      packages = Seq("org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.0"),
      commandArgs = Seq(brokerAddress, "subscribe", topic),
      kubernetesNamespace = testK8sNamespace,
      serviceAccount = serviceAccount)
    val sparkJobCluster = new SparkJobClusterDeployViaPod(conf)
    sparkJobCluster.start()
    // Since it is a streaming job, the cluster will keep running till we terminate it.
    assert(sparkJobCluster.isRunning(60),
      s"spark cluster did not start.")

    eventually(timeout(3.minutes), interval(20.seconds)) {
      val command =
        s"echo 'test-$topic' | kafka-console-producer.sh --topic $topic --broker-list $brokerAddress"
      val (r, s) = ClusterUtils.execCommand(getKafkaCluster.getPods.head, command, kubernetesClient)
      assert(s, s"Command $command should execute successfully: $r")
      // The deployer pod becomes the driver in client mode.
      val driverPod = sparkJobCluster.getPods.find(_.getMetadata.getName.contains("deploy")).get

      val fetchedDriverLog = kubernetesClient.pods().withName(driverPod.getMetadata.getName).getLog

      assert(fetchedDriverLog.contains(s"test-$topic"), "Should contain the result.")
    }

    // We close spark cluster only on successful completion of test, allows for debugging the failure.
    sparkJobCluster.stop()
  }


}

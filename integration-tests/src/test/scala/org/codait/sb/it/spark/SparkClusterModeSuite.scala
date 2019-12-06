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

import org.codait.sb.it.{TestSetup => ts}
import org.codait.sb.deploy.spark.{SparkJobClusterConfig, SparkJobClusterDeployViaPod}
import org.scalatest.concurrent.Eventually.{eventually, interval, timeout}

import scala.concurrent.duration._

class SparkClusterModeSuite extends SparkSuiteBase(false) {

  test("Run SparkPi example from Spark.") {
    val sparkPiClass = "org.apache.spark.examples.SparkPi"

    val sparkJobCluster = new SparkJobClusterDeployViaPod(
      SparkJobClusterConfig(name = "spi" + ts.testingPrefix,
        masterUrl = s"k8s://https://kubernetes.$testK8sNamespace.svc",
        sparkDeployMode = "cluster",
        className = sparkPiClass,
        sparkImage = sparkImagePath,
        pathToJar = examplesJar,
        numberOfExecutors = 2,
        commandArgs = Array("100"),
        imagePullPolicy = "IfNotPresent",
        kubernetesNamespace = testK8sNamespace,
        serviceAccount = serviceAccount))

    sparkJobCluster.start()
    eventually(timeout(6.minutes), interval(2.minutes)) {
      val driverPod = sparkJobCluster.getPods.filter(_.getMetadata.getName.contains("driver")).head
      assert(
        ts.kubernetesClient
          .pods().withName(driverPod.getMetadata.getName).getLog.contains("Pi is roughly 3.1"),
        "Should contain the result.")
    }

    // We close spark cluster only on successful completion of test, allows for debugging the failure.
    sparkJobCluster.stop()
  }

}

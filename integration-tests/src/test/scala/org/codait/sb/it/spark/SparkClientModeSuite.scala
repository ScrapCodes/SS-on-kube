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

class SparkClientModeSuite extends SparkSuiteBase {

  test("Run SparkPi example from Spark.") {
    val sparkPiClass = "org.apache.spark.examples.SparkPi"

    val sparkJobCluster = new SparkJobClusterDeployViaPod(SparkJobClusterConfig("spi" + ts.testingPrefix,
      s"k8s://https://kubernetes.$testK8sNamespace.svc",
      sparkDeployMode = "cluster",
      sparkPiClass,
      sparkImagePath,
      pathToJar = examplesJar,
      numberOfExecutors = 2,
      configParams = Map(),
      packages = Seq(),
      commandArgs = Array("100"),
      kubernetesNamespace = testK8sNamespace,
      serviceAccount = serviceAccount))

    sparkJobCluster.start()
    eventually(timeout(3.minutes), interval(20.seconds)) {
      val driverPod = sparkJobCluster.getPods.filter(_.getMetadata.getName.contains("driver")).head
      assert(
        ts.kubernetesClient
          .pods().withName(driverPod.getMetadata.getName).getLog.contains("Pi is roughly 3.1"),
        "Should contain the result.")
    }
    sparkJobCluster.stop()
  }

}

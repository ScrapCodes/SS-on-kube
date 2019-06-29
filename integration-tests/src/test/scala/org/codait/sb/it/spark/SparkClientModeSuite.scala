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

import org.codait.sb.deploy.spark.{SparkJobCluster, SparkJobClusterConfig}
import org.scalatest.BeforeAndAfterAll

class SparkClientModeSuite extends SparkSuiteBase with BeforeAndAfterAll {
  test("Run SparkPi example from Spark.") {

    val sparkPiClass = "org.apache.spark.examples.SparkPi"

    val sparkJobCluster = new SparkJobCluster(SparkJobClusterConfig("spi" + testingPrefix,
      "k8s://https://9.30.110.150:6443",
      deployMode = "cluster",
      sparkPiClass,
      sparkImagePath,
      serviceAccount,
      pathToJar = examplesJar,
      numberOfExecutors = 2,
      configParams = Map(),
      sparkHome = sparkHome,
      packages = Seq(),
      commandArgs = Array("100"),
      testK8sNamespace))

    sparkJobCluster.start()
    sparkJobCluster.waitUntilSparkDriverCompletes(120)

    val driverPod = sparkJobCluster.getPods.filter(_.getMetadata.getName.contains("driver")).head
    assert(
        k8sClient
        .pods().withName(driverPod.getMetadata.getName).getLog.contains("Pi is roughly 3.1"),
      "Should contain the result.")
  }

}

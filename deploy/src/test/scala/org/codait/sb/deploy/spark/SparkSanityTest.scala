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

import org.scalatest.{BeforeAndAfterAll, FunSuite}

class SparkSanityTest extends FunSuite with BeforeAndAfterAll {

  private val sparkImagePath: String = "scrapcodes/spark:v2.4.0"

  test("Run SparkPi example from Spark.") {
    val sparkHome = "/Users/prashant/Work/spark-2.4.0-bin-hadoop2.7"
    System.setProperty("sb.spark.home", sparkHome)
    System.setProperty("sb.kubernetes.master", "9.30.110.150:6443")
    val examplesJar = "/opt/spark/examples/jars/spark-examples_2.11-2.4.0.jar"
    val sparkPiClass = "org.apache.spark.examples.SparkPi"
    val serviceAccount = "spark"
    SparkCluster.start("SparkSanityTest",
      sparkPiClass,
      sparkImagePath,
      serviceAccount,
      examplesJar, timeoutSeconds = 60)
  }

}

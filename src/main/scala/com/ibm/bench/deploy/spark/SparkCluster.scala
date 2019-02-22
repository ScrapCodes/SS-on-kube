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

package com.ibm.bench.deploy.spark

import java.io.File

object SparkCluster {

  def start(): Unit = {
    val sparkHome: Option[String] =
      Option(System.getenv("SPARK_HOME")).orElse(Option(System.getProperty("sb.spark.home")))
    val kubeConfig: Option[String] = Option(System.getenv("KUBECONFIG"))
    assert(sparkHome.isDefined,
      "Please export spark home as SPARK_HOME or specify the sb.spark.home configuration.")
    assert(kubeConfig.isDefined, "Please export path to kubernetes config as kubeConfig env variable.")

    val sparkSubmitPath = sparkHome.get() + "/bin/spark-submit"
    // In case a user has set a wrong path to SPARK_HOME.
    assert(new File(sparkSubmitPath).exists(),
      s"Please specify the correct value for SPARK_HOME, $sparkSubmitPath path not found.")

    new ProcessBuilder().command(sparkSubmitPath, "--")
  }

}

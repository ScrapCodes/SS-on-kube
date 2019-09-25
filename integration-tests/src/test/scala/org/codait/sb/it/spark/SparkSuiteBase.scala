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

import org.codait.sb.it.TestBase

private[spark] class SparkSuiteBase extends TestBase {
  // TODO: following should be picked up from configuration.
  val sparkImagePath: String = "scrapcodes/spark:v2.4.3"
  val testK8sNamespace = "default"
  val serviceAccount = "spark"
  val examplesJar = "/opt/spark/examples/jars/spark-examples_2.11-2.4.3.jar"
}

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

private[spark] class SparkSuiteBase(kafka: Boolean = true) extends TestBase(kafka) {
  // TODO: following should be picked up from configuration.
  val sparkVersion: String = "3.0.0-preview"
  val sparkKafkaPackage: String = "org.apache.spark:spark-sql-kafka-0-10_2.12:3.0.0-preview"
  val sparkImagePath: String = s"scrapcodes/spark:v$sparkVersion"
  val examplesJar = s"local:///opt/spark/examples/jars/spark-examples_2.12-$sparkVersion.jar"
}

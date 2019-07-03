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

import java.util.UUID

import io.fabric8.kubernetes.client.{DefaultKubernetesClient, NamespacedKubernetesClient}
import org.codait.sb.util.SBConfig
import org.scalatest.FunSuite
import org.slf4j.{Logger, LoggerFactory}

private[spark] class SparkSuiteBase extends FunSuite {
  val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  //TODO: Have a proper client factory, which loads all configuration specified by the user.
  lazy val k8sClient: NamespacedKubernetesClient =
    new DefaultKubernetesClient().inNamespace(SBConfig.NAMESPACE)
  // TODO: following should be picked up from configuration.
  val sparkImagePath: String = "scrapcodes/spark:v2.4.0"
  val testingPrefix = s"${UUID.randomUUID().toString.takeRight(5)}"
  val testK8sNamespace = "default"
  val serviceAccount = "spark"
  // val sparkHome = "/Users/prashant/Work/spark-2.4.0-bin-hadoop2.7"
  val examplesJar = "/opt/spark/examples/jars/spark-examples_2.11-2.4.0.jar"
  //val localExamplesJar = s"$sparkHome/examples/jars/spark-examples_2.11-2.4.0.jar"
  val existingKafkaService = ""
  val existingKafkaPodName = ""
}

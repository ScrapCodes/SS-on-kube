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

package org.codait.sb.deploy.kafka


object Constants {
  val KAFKA_BROKER_PORT_NAME: String = "kafka-broker"
  val KAFKA_BROKER_PORT = 9092
  val KAFKA_CONTAINER_IMAGE_NAME = s"bitnami/kafka:2.1.0"
}

private [kafka]
object Helpers {
  def kafkaServiceName(prefix: String) = s"${prefix}kafka-service"
  def kafkaStatefulSetName(prefix: String) = s"${prefix}kafka"
  def kafkaContainerName(prefix: String) = s"${prefix}bitnami-kafka"
}
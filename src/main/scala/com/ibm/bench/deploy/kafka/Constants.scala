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

package com.ibm.bench.deploy.kafka

import com.ibm.bench.util.SBConfig

object Constants {
  val KAFKA_BROKER_PORT_NAME: String = "kafka-broker"
  val KAFKA_BROKER_PORT = 9092
  val KAFKA_SERVICE_NAME = s"${SBConfig.PREFIX}kafka-service"
  val KAFKA_STATEFUL_SET_NAME = s"${SBConfig.PREFIX}kafka"
  val KAFKA_CONTAINER_NAME = s"${SBConfig.PREFIX}bitnami-kafka"
  val KAFKA_CONTAINER_IMAGE_NAME = s"bitnami/kafka:2.1.0"

}

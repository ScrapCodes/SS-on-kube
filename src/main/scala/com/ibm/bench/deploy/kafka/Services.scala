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

import java.util

import io.fabric8.kubernetes.api.model.{Service, ServiceBuilder}

import scala.collection.JavaConverters._

object Services {

  val labels: util.Map[String, String] = Map("app" -> "kafka").asJava

  val brokerService: Service = new ServiceBuilder()
    .withNewMetadata()
      .withName(Constants.KAFKA_SERVICE_NAME)
      .withLabels(labels)
      .endMetadata()
    .withNewSpec()
      .withSelector(labels)
      .addNewPort()
        .withName(Constants.KAFKA_BROKER_PORT_NAME)
        .withPort(Constants.KAFKA_BROKER_PORT)
        .withNewTargetPort(Constants.KAFKA_BROKER_PORT)
        .endPort()
      .endSpec()
    .build()

}

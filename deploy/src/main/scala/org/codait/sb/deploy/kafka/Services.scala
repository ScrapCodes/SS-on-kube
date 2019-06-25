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

import java.util

import io.fabric8.kubernetes.api.model.{Service, ServiceBuilder}

import scala.collection.JavaConverters._

private [kafka]
object Services {

  def labels(prefix: String): util.Map[String, String] =
    Map("app" -> s"kafka$prefix").asJava

  def brokerService(prefix: String): Service = new ServiceBuilder()
    .withNewMetadata()
      .withName(Helpers.kafkaServiceName(prefix))
      .withLabels(labels(prefix))
      .endMetadata()
    .withNewSpec()
      .withSelector(labels(prefix))
      .addNewPort()
        .withName(Constants.KAFKA_BROKER_PORT_NAME)
        .withPort(Constants.KAFKA_BROKER_PORT)
        .withNewTargetPort(Constants.KAFKA_BROKER_PORT)
        .endPort()
      .endSpec()
    .build()

}

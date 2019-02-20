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

import com.ibm.bench.deploy.{zookeeper => zk}
import io.fabric8.kubernetes.api.model.{Container, ContainerBuilder, EnvVarBuilder, QuantityBuilder}

object KafkaContainer {

  private val cpuQuantityRequest = new QuantityBuilder()
    .withAmount("2")
    .build()

  private val memoryQuantityRequest = new QuantityBuilder()
    .withAmount("2Gi")
    .build()

  private val cpuQuantityLimit = new QuantityBuilder()
    .withAmount("4")
    .build()

  private val memoryQuantityLimit = new QuantityBuilder()
    .withAmount("8Gi")
    .build()

  private val envVar1 = new EnvVarBuilder()
    .withName("ALLOW_PLAINTEXT_LISTENER")
    .withValue("yes")
    .build()

  private val envVar2 = new EnvVarBuilder()
    .withName("KAFKA_ZOOKEEPER_CONNECT")
    .withValue(s"${zk.Constants.ZK_CLIENT_SERVICE_NAME}:${zk.Constants.ZK_CLIENT_PORT}")
    .build()

  private[bench] val container: Container = new ContainerBuilder()
    .withName(Constants.KAFKA_CONTAINER_NAME)
    .withImage(Constants.KAFKA_CONTAINER_IMAGE_NAME)
    .withImagePullPolicy("Always")
    .addNewPort()
      .withName(Constants.KAFKA_BROKER_PORT_NAME)
      .withContainerPort(Constants.KAFKA_BROKER_PORT)
      .withProtocol("TCP")
      .endPort()
    .withEnv(envVar1, envVar2)
    .withNewReadinessProbe()
      .withNewTcpSocket()
        .withNewPort(Constants.KAFKA_BROKER_PORT)
        .endTcpSocket()
      .withInitialDelaySeconds(60)
      .withTimeoutSeconds(5)
      .withPeriodSeconds(20)
      .withFailureThreshold(10)
      .endReadinessProbe()
    .withNewLivenessProbe()
      .withNewTcpSocket()
        .withNewPort(Constants.KAFKA_BROKER_PORT)
        .endTcpSocket()
      .withInitialDelaySeconds(60)
      .withTimeoutSeconds(5)
      .endLivenessProbe()
    .editOrNewResources()
      .addToRequests("memory", memoryQuantityRequest)
      .addToLimits("memory", memoryQuantityLimit)
      .addToRequests("cpu", cpuQuantityRequest)
      .addToLimits("cpu", cpuQuantityLimit)
      .endResources()
    .build()

}

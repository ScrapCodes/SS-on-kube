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

import org.codait.sb.deploy.{zookeeper => zk}
import io.fabric8.kubernetes.api.model.{Container, ContainerBuilder, EnvVarBuilder, QuantityBuilder}

private [kafka]
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

  private val allowPlainTextEnvVar = new EnvVarBuilder()
    .withName("ALLOW_PLAINTEXT_LISTENER")
    .withValue("yes")
    .build()

  private val kafkaZookeeperConnectEnvVar = new EnvVarBuilder()
    .withName("KAFKA_ZOOKEEPER_CONNECT")
    .withValue(s"${zk.Constants.ZK_CLIENT_SERVICE_NAME}:${zk.Constants.ZK_CLIENT_PORT}")
    .build()

  private val kafkaTopicEnvVar = new EnvVarBuilder()
    .withName("KAFKA_DELETE_TOPIC_ENABLE")
    .withValue(s"true")
    .build()

  private[kafka] val container: Container = new ContainerBuilder()
    .withName(Constants.KAFKA_CONTAINER_NAME)
    .withImage(Constants.KAFKA_CONTAINER_IMAGE_NAME)
    .addNewPort()
      .withName(Constants.KAFKA_BROKER_PORT_NAME)
      .withContainerPort(Constants.KAFKA_BROKER_PORT)
      .withProtocol("TCP")
      .endPort()
    .withEnv(allowPlainTextEnvVar, kafkaZookeeperConnectEnvVar, kafkaTopicEnvVar)
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

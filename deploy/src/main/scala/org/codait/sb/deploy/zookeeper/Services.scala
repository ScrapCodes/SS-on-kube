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

package org.codait.sb.deploy.zookeeper

import java.util

import io.fabric8.kubernetes.api.model.{Service, ServiceBuilder}

import scala.collection.JavaConverters._

private[zookeeper]
object Services {

  def labels(prefix: String): util.Map[String, String] = Map("app" -> s"zk$prefix").asJava

  def internalService(prefix: String): Service = new ServiceBuilder()
    .withNewMetadata()
      .withName(Helpers.zkInternalServiceName(prefix))
      .withLabels(labels(prefix))
      .endMetadata()
    .withNewSpec()
      .withClusterIP("None")
      .withSelector(labels(prefix))
      .addNewPort()
        .withName("server")
        .withPort(Constants.ZK_SERVER_PORT)
        .withNewTargetPort(Constants.ZK_SERVER_PORT)
        .endPort()
      .addNewPort()
        .withName("leader-election")
        .withPort(Constants.ZK_ELECTION_PORT)
        .withNewTargetPort(Constants.ZK_ELECTION_PORT)
        .endPort()
    .endSpec()
    .build()

  def clientService(prefix: String): Service = new ServiceBuilder()
    .withNewMetadata()
      .withName(Helpers.zkClientServiceName(prefix))
      .withLabels(labels(prefix))
      .endMetadata()
    .withNewSpec()
      .withSelector(labels(prefix))
      .addNewPort()
        .withName("client")
        .withPort(Constants.ZK_CLIENT_PORT)
        .withNewTargetPort(Constants.ZK_CLIENT_PORT)
        .endPort()
    .endSpec()
    .build()

}

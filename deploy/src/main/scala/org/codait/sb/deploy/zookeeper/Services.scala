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

  val labels: util.Map[String, String] = Map("app" -> "zk").asJava

  val internalService: Service = new ServiceBuilder()
    .withNewMetadata()
      .withName(Constants.ZK_INTERNAL_SERVICE_NAME)
      .withLabels(labels)
      .endMetadata()
    .withNewSpec()
      .withClusterIP("None")
      .withSelector(labels)
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


  val clientService: Service = new ServiceBuilder()
    .withNewMetadata()
      .withName(Constants.ZK_CLIENT_SERVICE_NAME)
      .withLabels(labels)
      .endMetadata()
    .withNewSpec()
      .withSelector(labels)
      .addNewPort()
        .withName("client")
        .withPort(Constants.ZK_CLIENT_PORT)
        .withNewTargetPort(Constants.ZK_CLIENT_PORT)
        .endPort()
    .endSpec()
    .build()

}

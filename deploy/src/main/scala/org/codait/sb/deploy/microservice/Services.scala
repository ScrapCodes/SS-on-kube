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

package org.codait.sb.deploy.microservice

import io.fabric8.kubernetes.api.model.{Service, ServiceBuilder}

import scala.collection.JavaConverters._

abstract class AddServiceFeature {
  def addFeature(cb: ServiceBuilder): ServiceBuilder
}

object Services {
  def labels(prefix: String): Map[String, String] =
    Map("app" -> s"micro-svc-$prefix")

  private def addPorts(b: ServiceBuilder, config: MicroServiceClusterConfig): ServiceBuilder = {
    val spec = b.editOrNewSpec()

    val configuredPorts = config.namedServicePorts.foldLeft(spec) {
      case (sb, (portName: String, port: Int)) =>
        sb.addNewPort().withName(portName).withPort(port).withNewTargetPort(port).endPort()
    }
    configuredPorts.endSpec()
  }

  def generate(config: MicroServiceClusterConfig): Service = {
    var sb = new ServiceBuilder().withNewMetadata()
      .withName(s"${config.clusterPrefix}-${config.clusterName}-svc")
      .withLabels((labels(config.clusterPrefix) ++ config.extraLabels).asJava)
      .endMetadata()
      .editOrNewSpec()
      .withType("NodePort")
      .withSelector(labels(config.clusterPrefix).asJava).endSpec()

    sb = addPorts(sb, config)
    sb = config.userServiceFeatures.foldLeft(sb) {
      (b: ServiceBuilder, op: AddServiceFeature) =>
        op.addFeature(b)
    }

    sb.build()
  }
}

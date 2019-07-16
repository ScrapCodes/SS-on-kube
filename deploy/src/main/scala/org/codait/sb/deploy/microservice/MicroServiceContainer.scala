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

import io.fabric8.kubernetes.api.model._

import scala.collection.JavaConverters._


abstract class AddContainerFeature {
  def addFeature(cb: ContainerBuilder): ContainerBuilder
}

object MicroServiceContainer {

  private def addContainerPorts(container: ContainerBuilder,
                                config: MicroServiceClusterConfig): ContainerBuilder = {
    val containerPorts =
      config.namedServicePorts.map { case (portName: String, port: Int) => new ContainerPortBuilder()
        .withName(portName)
        .withContainerPort(port)
        .withProtocol("TCP").build()
      }
    containerPorts.foldLeft(container) {
      case (c: ContainerBuilder, cp: ContainerPort) =>
        c.withPorts(cp)
    }
  }

  private def addContainerCommand(container: ContainerBuilder,
                                  config: MicroServiceClusterConfig): ContainerBuilder = {
    config.containerCommand.foldLeft(container) {
      case (c: ContainerBuilder, x: Seq[String]) =>
        c.withCommand(x.toList.asJava)
    }
  }

  private def addContainerProbes(container: ContainerBuilder,
                                 config: MicroServiceClusterConfig): ContainerBuilder = {
    def livenessProbe(port: Int) = new ProbeBuilder()
      .withInitialDelaySeconds(10)
      .withFailureThreshold(3)
      .withTimeoutSeconds(10)
      .withNewTcpSocket().withNewPort(port).endTcpSocket().build()

    def readinessProbe(port: Int) = new ProbeBuilder()
      .withInitialDelaySeconds(10)
      .withFailureThreshold(3)
      .withTimeoutSeconds(10)
      .withNewTcpSocket()
      .withNewPort(port).endTcpSocket().build()

    val c: ContainerBuilder =
      config.namedServicePorts.foldLeft(container) {
        case (x: ContainerBuilder, (s: String, port: Int)) =>
          x.withReadinessProbe(readinessProbe(port))
            .withLivenessProbe(livenessProbe(port))
      }
    c
  }

  private def addResourceConstraints(containerBuilder: ContainerBuilder,
                                     config: MicroServiceClusterConfig): ContainerBuilder = {
    def quantity(s: String) = new QuantityBuilder()
      .withAmount(s)
      .build()

    containerBuilder.editOrNewResources()
      .addToRequests("memory", quantity("1Gi"))
      .addToLimits("memory", quantity("4Gi"))
      .addToRequests("cpu", quantity("2"))
      .addToLimits("cpu", quantity("4"))
      .endResources()
  }

  private def addEnvVars(containerBuilder: ContainerBuilder,
                         config: MicroServiceClusterConfig): ContainerBuilder = {
    def envVars(name: String, value: String) = {
      new EnvVarBuilder().withName(name).withValue(value).build()
    }

    config.envVars.foldLeft(containerBuilder) {
      case (c: ContainerBuilder, (name: String, value: String)) =>
        c.withEnv(envVars(name, value))
    }
  }

  def container(config: MicroServiceClusterConfig): Container = {
    val containerName = s"${config.clusterPrefix}-${config.clusterName}"
    require(containerName.length < 63,
      "Combined length of cluster prefix and name cannot be more than 62.")
    var c: ContainerBuilder = new ContainerBuilder()
      .withName(containerName)
      .withImage(config.microServiceImage)
    c = addContainerCommand(c, config)
    c = addContainerPorts(c, config)
    c = addContainerProbes(c, config)
    c = addResourceConstraints(c, config)
    c = addEnvVars(c, config)
    c = config.userContainerFeatures.foldLeft(c) {
      case (c: ContainerBuilder, op: AddContainerFeature) =>
        op.addFeature(c)
    }
    c.build()
  }

}

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

import io.fabric8.kubernetes.api.model.{Container, ContainerBuilder, QuantityBuilder}

import scala.collection.JavaConverters._

private[zookeeper]
object ZKContainer {

  private val cpuQuantityRequest = new QuantityBuilder()
    .withAmount("0.5")
    .build()

  private val memoryQuantityRequest = new QuantityBuilder()
    .withAmount("1Gi")
    .build()

  private val cpuQuantityLimit = new QuantityBuilder()
    .withAmount("1.5")
    .build()

  private val memoryQuantityLimit = new QuantityBuilder()
    .withAmount("2Gi")
    .build()

  private val startCommand = Seq("sh", "-c",
    Seq("start-zookeeper", "--servers=3",
    "--data_dir=/var/lib/zookeeper/data",
    "--data_log_dir=/var/lib/zookeeper/data/log",
    "--conf_dir=/opt/zookeeper/conf",
    s"--client_port=${Constants.ZK_CLIENT_PORT}",
    s"--election_port=${Constants.ZK_ELECTION_PORT}",
    s"--server_port=${Constants.ZK_SERVER_PORT}",
    "--tick_time=2000",
    "--init_limit=10",
    "--sync_limit=5",
    "--heap=512M",
    "--max_client_cnxns=60",
    "--snap_retain_count=3",
    "--purge_interval=12",
    "--max_session_timeout=40000",
    "--min_session_timeout=4000",
    "--log_level=INFO").mkString(" ")).asJava

  private val livenessProbeCommand =
    Seq("sh", "-c", s"zookeeper-ready ${Constants.ZK_CLIENT_PORT}").asJava

  private def readinessProbeCommand =
    Seq("sh", "-c", "zkCli.sh create /test$(date -j \"+%H%M%S\") 1").asJava

  def container(prefix: String): Container = new ContainerBuilder()
    .withName(Helpers.zkContainerName(prefix))
    .withImage(Constants.ZK_IMAGE)
    .addNewPort()
      .withName("client")
      .withContainerPort(Constants.ZK_CLIENT_PORT)
      .withProtocol("TCP")
      .endPort()
    .addNewPort()
      .withName("server")
      .withContainerPort(Constants.ZK_SERVER_PORT)
      .withProtocol("TCP")
      .endPort()
    .addNewPort()
      .withName("leader-election")
      .withContainerPort(Constants.ZK_ELECTION_PORT)
      .withProtocol("TCP")
      .endPort()
    .addAllToCommand(startCommand)
    .withNewReadinessProbe()
      .withNewExec()
        .addAllToCommand(livenessProbeCommand)
        .endExec()
      .withInitialDelaySeconds(60)
      .withTimeoutSeconds(5)
      .withPeriodSeconds(20)
      .withFailureThreshold(10)
      .endReadinessProbe()
    .withNewLivenessProbe()
      .withNewExec()
        .addAllToCommand(livenessProbeCommand)
        .endExec()
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

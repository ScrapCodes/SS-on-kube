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

import io.fabric8.kubernetes.api.model.{Pod, ServicePort}
import org.codait.sb.deploy.Cluster

import scala.collection.JavaConverters._

class MicroServiceCluster(override val clusterConfig: MicroServiceClusterConfig) extends Cluster {
  /**
    * Addresses of the services exposed by this cluster.
    *
    * @return Map of "Port Name"-internal -> "Full service address internal" and
    *         Map of "Port Name"-external -> "Full service address external".
    */
  override def serviceAddresses: Map[String, String] = {
    val internalName = Services.generate(clusterConfig).getMetadata.getName
    val svc = Cluster.kubernetesClient.services().withName(internalName)
    val nodeIP =
      Cluster.kubernetesClient.pods()
        .withLabels(Services.labels(clusterConfig.clusterPrefix).asJava)
        .list().getItems.get(0).getStatus.getHostIP

    val externalName = s"$nodeIP"

    svc.get().getSpec.getPorts.asScala.foldLeft(Map[String, String]()) {
      case (m: Map[String, String], sp: ServicePort) =>
        m ++ Map[String, String](
          s"${sp.getName}-internal" -> s"$internalName:${sp.getPort}",
          s"${sp.getName}-external" -> s"$externalName:${sp.getNodePort}")
    }
  }

  override def getPods: Seq[Pod] = {
    Cluster.kubernetesClient.pods()
      .withLabels(Services.labels(clusterConfig.clusterPrefix).asJava).list().getItems.asScala
  }

  override def start(): Unit = {
    Cluster.kubernetesClient.services().createOrReplace(Services.generate(clusterConfig))
    Cluster.kubernetesClient.apps().replicaSets()
      .createOrReplace(MicroServiceReplicaSet.replicaSet(clusterConfig))
    if (clusterConfig.enableHorizontalPodAutoscaler &&
      !Cluster.kubernetesClient.getVersion.getMinor.contains("15")) {
      // Currently not supported for kubernetes version >=1.15
      Cluster.kubernetesClient.autoscaling().horizontalPodAutoscalers()
        .createOrReplace(MicroServiceReplicaSet.hpa(clusterConfig))
    }
  }

  override def stop(): Unit = {
    Cluster.kubernetesClient.apps().replicaSets()
      .delete(MicroServiceReplicaSet.replicaSet(clusterConfig))
    Cluster.kubernetesClient.services().delete(Services.generate(clusterConfig))
    Cluster.kubernetesClient.pods()
      .withLabels(Services.labels(clusterConfig.clusterPrefix).asJava).delete()
    if (clusterConfig.enableHorizontalPodAutoscaler &&
      !Cluster.kubernetesClient.getVersion.getMinor.contains("15")) {
      Cluster.kubernetesClient.autoscaling().horizontalPodAutoscalers()
        .delete(MicroServiceReplicaSet.hpa(clusterConfig))
    }
  }

  override def isRunning(timeoutSeconds: Int): Boolean = {
    getPods.exists { x =>
      x.getStatus.getPhase.equalsIgnoreCase("running") &&
      x.getStatus.getContainerStatuses.get(0).getReady // since we have only one container.
    }
  }
}

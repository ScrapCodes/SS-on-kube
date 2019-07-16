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
import io.fabric8.kubernetes.api.model.apps.{ReplicaSet, ReplicaSetBuilder}

import scala.collection.JavaConverters._

object MicroServiceReplicaSet {

  private def podAffinityTerm(prefix: String): PodAffinityTerm = new PodAffinityTermBuilder()
    .withNewLabelSelector()
    .addToMatchLabels(Services.labels(prefix).asJava)
    .endLabelSelector()
    .withTopologyKey("kubernetes.io/hostname")
    .build()

  private def weightedPodAffinityTerm(prefix: String) =
    new WeightedPodAffinityTermBuilder()
      .withWeight(10)
      .withPodAffinityTerm(podAffinityTerm(prefix))
      .build()

  private val securityContext = new PodSecurityContextBuilder()
    .withRunAsUser(1100l)
    .withFsGroup(1100l)
    .build()

  private def affinity(prefix: String): Affinity = new AffinityBuilder()
    .withNewPodAntiAffinity()
    .withPreferredDuringSchedulingIgnoredDuringExecution(weightedPodAffinityTerm(prefix))
    .endPodAntiAffinity()
    .build()

  def replicaSet(config: MicroServiceClusterConfig): ReplicaSet = {
    val name = s"${config.clusterPrefix}-${config.clusterName}"

    new ReplicaSetBuilder()
      .withApiVersion("apps/v1")
      .withKind("ReplicaSet")
      .withNewMetadata()
      .withName(name)
      .endMetadata()
      .withNewSpec()
        .withReplicas(config.initialReplicaSize)
        .withNewSelector()
          .addToMatchLabels(Services.labels(config.clusterPrefix).asJava)
          .endSelector()
        .withNewTemplate()
          .withNewMetadata()
            .withLabels((Services.labels(config.clusterPrefix) ++ config.extraLabels).asJava)
            .endMetadata()
          .withNewSpec()
            .withServiceAccount(config.serviceAccount)
            .withServiceAccountName(config.serviceAccount)
            .withAffinity(affinity(config.clusterPrefix))
            .withContainers(MicroServiceContainer.container(config))
            .withSecurityContext(securityContext)
            .endSpec()
          .endTemplate()
        .endSpec()
      .build()
  }
}

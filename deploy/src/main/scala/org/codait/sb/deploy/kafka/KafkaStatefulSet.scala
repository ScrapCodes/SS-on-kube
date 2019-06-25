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

import io.fabric8.kubernetes.api.model._
import io.fabric8.kubernetes.api.model.apps.{StatefulSet, StatefulSetBuilder}

private [kafka]
object KafkaStatefulSet {

  private def podAffinityTerm(prefix: String): PodAffinityTerm = new PodAffinityTermBuilder()
    .withNewLabelSelector()
      .addToMatchLabels(Services.labels(prefix))
      .endLabelSelector()
    .withTopologyKey("kubernetes.io/hostname")
    .build()

  private def weightedPodAffinityTerm(prefix: String) =
    new WeightedPodAffinityTermBuilder()
    .withNewWeight(10)
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

  def statefulSet(config: KafkaClusterConfig): StatefulSet = new StatefulSetBuilder()
    .withApiVersion("apps/v1")
    .withKind("StatefulSet")
    .withNewMetadata()
      .withName(Helpers.kafkaStatefulSetName(config.clusterPrefix))
      .endMetadata()
    .withNewSpec()
    .withReplicas(config.replicaSize)
      .withServiceName(Services.brokerService(config.clusterPrefix).getMetadata.getName)
    .withNewSelector()
      .addToMatchLabels(Services.labels(config.clusterPrefix))
      .endSelector()
    .withPodManagementPolicy("Parallel")
    .withNewTemplate()
      .withNewMetadata()
        .withLabels(Services.labels(config.clusterPrefix))
        .endMetadata()
      .withNewSpec()
        .withAffinity(affinity(config.clusterPrefix))
        .withContainers(KafkaContainer.container(config.clusterPrefix, config.zookeeperAddress))
        .withSecurityContext(securityContext)
        .endSpec()
      .endTemplate()
    .endSpec()
    .build()
}

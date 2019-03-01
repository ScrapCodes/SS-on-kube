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

  private val podAffinityTerm: PodAffinityTerm = new PodAffinityTermBuilder()
    .withNewLabelSelector()
      .addToMatchLabels(Services.labels)
      .endLabelSelector()
    .withTopologyKey("kubernetes.io/hostname")
    .build()

  private val weightedPodAffinityTerm = new WeightedPodAffinityTermBuilder()
    .withNewWeight(10)
    .withPodAffinityTerm(podAffinityTerm)
    .build()

  private val securityContext = new PodSecurityContextBuilder()
    .withRunAsUser(1100l)
    .withFsGroup(1100l)
    .build()

  private val affinity: Affinity = new AffinityBuilder()
    .withNewPodAntiAffinity()
    .withPreferredDuringSchedulingIgnoredDuringExecution(weightedPodAffinityTerm)
      .endPodAntiAffinity()
    .build()

  val statefulSet: StatefulSet = new StatefulSetBuilder()
    .withApiVersion("apps/v1")
    .withKind("StatefulSet")
    .withNewMetadata()
      .withName(Constants.KAFKA_STATEFUL_SET_NAME)
      .endMetadata()
    .withNewSpec()
    .withReplicas(3)
      .withServiceName(Services.brokerService.getMetadata.getName)
    .withNewSelector()
      .addToMatchLabels(Services.labels)
      .endSelector()
    .withPodManagementPolicy("Parallel")
    .withNewTemplate()
      .withNewMetadata()
        .withLabels(Services.labels)
        .endMetadata()
      .withNewSpec()
        .withAffinity(affinity)
        .withContainers(KafkaContainer.container)
        .withSecurityContext(securityContext)
        .endSpec()
      .endTemplate()
    .endSpec()
    .build()
}

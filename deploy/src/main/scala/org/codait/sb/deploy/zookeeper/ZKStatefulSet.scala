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

import io.fabric8.kubernetes.api.model._
import io.fabric8.kubernetes.api.model.apps.{StatefulSet, StatefulSetBuilder}

private[zookeeper]
object ZKStatefulSet {

  private val podAffinityTerm = new PodAffinityTermBuilder()
    .withNewLabelSelector()
    .addToMatchLabels(Services.labels)
      .endLabelSelector()
    .withTopologyKey("kubernetes.io/hostname")
    .build()

  private[zookeeper] val securityContext = new PodSecurityContextBuilder()
    .withRunAsUser(1000l)
    .withFsGroup(1000l)
    .build()

  val affinity: Affinity = new AffinityBuilder()
    .withNewPodAntiAffinity()
      .withRequiredDuringSchedulingIgnoredDuringExecution(podAffinityTerm)
      .endPodAntiAffinity()
    .build()

  val statefulSet: StatefulSet = new StatefulSetBuilder()
    .withApiVersion("apps/v1")
    .withKind("StatefulSet")
    .withNewMetadata()
      .withName(Constants.ZK_STATEFUL_SET_NAME)
    .endMetadata()
    .withNewSpec()
      .withReplicas(3)
      .withServiceName(Services.internalService.getMetadata.getName)
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
        .withContainers(ZKContainer.container)
        .withSecurityContext(securityContext)
        .endSpec()
      .endTemplate()
    .endSpec()
    .build()

}

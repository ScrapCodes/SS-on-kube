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

  private def podAffinityTerm(prefix: String) = new PodAffinityTermBuilder()
    .withNewLabelSelector()
    .addToMatchLabels(Services.labels(prefix))
      .endLabelSelector()
    .withTopologyKey("kubernetes.io/hostname")
    .build()

  private[zookeeper] val securityContext = new PodSecurityContextBuilder()
    .withRunAsUser(1000l)
    .withFsGroup(1000l)
    .build()

  def statefulSet(prefix: String, replicaSize: Int): StatefulSet = new StatefulSetBuilder()
    .withApiVersion("apps/v1")
    .withKind("StatefulSet")
    .withNewMetadata()
      .withName(Helpers.zkStatefulSetName(prefix))
    .endMetadata()
    .withNewSpec()
      .withReplicas(replicaSize)
      .withServiceName(Services.internalService(prefix).getMetadata.getName)
    .withNewSelector()
      .addToMatchLabels(Services.labels(prefix))
      .endSelector()
    .withPodManagementPolicy("Parallel")
    .withNewTemplate()
      .withNewMetadata()
        .withLabels(Services.labels(prefix))
        .endMetadata()
      .withNewSpec()
        .withNewAffinity()
          .withNewPodAntiAffinity()
          .withRequiredDuringSchedulingIgnoredDuringExecution(podAffinityTerm(prefix))
          .endPodAntiAffinity()
        .endAffinity()
        .withContainers(ZKContainer.container(prefix))
        .withSecurityContext(securityContext)
        .endSpec()
      .endTemplate()
    .endSpec()
    .build()

}

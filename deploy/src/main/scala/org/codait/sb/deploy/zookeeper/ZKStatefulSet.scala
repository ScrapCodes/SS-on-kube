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

  def statefulSet(config: ZKClusterConfig): StatefulSet = new StatefulSetBuilder()
    .withApiVersion("apps/v1")
    .withKind("StatefulSet")
    .withNewMetadata()
      .withName(Helpers.zkStatefulSetName(config.clusterPrefix))
    .endMetadata()
    .withNewSpec()
      .withReplicas(config.replicaSize)
      .withServiceName(Services.internalService(config.clusterPrefix).getMetadata.getName)
    .withNewSelector()
      .addToMatchLabels(Services.labels(config.clusterPrefix))
      .endSelector()
    .withPodManagementPolicy("Parallel")
    .withNewTemplate()
      .withNewMetadata()
        .withLabels(Services.labels(config.clusterPrefix))
        .endMetadata()
      .withNewSpec()
        .withServiceAccount(config.serviceAccount)
        .withServiceAccountName(config.serviceAccount)
        .withNewAffinity()
          .withNewPodAntiAffinity()
          .withRequiredDuringSchedulingIgnoredDuringExecution(podAffinityTerm(config.clusterPrefix))
          .endPodAntiAffinity()
        .endAffinity()
        .withContainers(ZKContainer.container(config))
        .withSecurityContext(securityContext)
        .endSpec()
      .endTemplate()
    .endSpec()
    .build()

}

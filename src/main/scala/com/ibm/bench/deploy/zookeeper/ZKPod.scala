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

package com.ibm.bench.deploy.zookeeper

import io.fabric8.kubernetes.api.model.{Pod, PodBuilder}
import io.fabric8.kubernetes.client.KubernetesClient

/** Create a zookeeper pod, can be used incase we want to directly deploy pods.*/
object ZKPod {
  private def createPod(ordinal: Int): Pod =
    new PodBuilder()
      .withApiVersion("v1")
      .withKind("Pod")
      .editOrNewMetadata()
        .withName(s"${Constants.ZK_POD_NAME}-$ordinal")
        .withLabels(Services.labels)
      .endMetadata()
    .withNewSpec()
      .withHostname(s"${Constants.ZK_POD_NAME}-$ordinal")
      .withSubdomain(Services.internalService.getMetadata.getName)
      .withAffinity(ZKStatefulSet.affinity)
      .withSecurityContext(ZKStatefulSet.securityContext)
      .withContainers(ZKContainer.container)
      .endSpec()
    .build()

  def deploy(client: KubernetesClient, count: Int): Unit = {
    if (count < 3) {
      println("[Warn] A count of less than three can not tolerate server failure. ")
    }
    for (i <- 0 until count) yield client.pods().createOrReplace(createPod(i))
  }

}

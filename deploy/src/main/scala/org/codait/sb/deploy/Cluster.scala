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

package org.codait.sb.deploy

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.{DefaultKubernetesClient, NamespacedKubernetesClient}
import org.codait.sb.util.SBConfig

trait Cluster {

  //TODO: Have a proper client factory, which loads all configuration specified by the user.
  protected lazy val kubernetesClient: NamespacedKubernetesClient =
    new DefaultKubernetesClient().inNamespace(SBConfig.NAMESPACE)

  /**
    * Address of the service exposed by this cluster.
    * TODO: Should be plural, incase the cluster exposes multiple services.
    */
  val serviceAddress: String

  val clusterConfig: ClusterConfig

  def getPods: Seq[Pod]

  def start(): Unit

  def stop(): Unit

  def isRunning(timeoutSeconds: Int): Boolean
}

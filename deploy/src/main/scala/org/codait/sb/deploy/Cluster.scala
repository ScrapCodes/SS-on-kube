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

  /**
    * Addresses of the services exposed by this cluster.
    */
  def serviceAddresses: Array[ServiceAddresses]

  val clusterConfig: ClusterConfig

  def getPods: Seq[Pod]

  def start(): Unit

  def stop(): Unit

  def isRunning(timeoutSeconds: Int): Boolean
}

object Cluster {

  //TODO: Have a proper client factory, which loads all configuration specified by the user.
  private[deploy] lazy val kubernetesClient: NamespacedKubernetesClient =
    new DefaultKubernetesClient().inNamespace(SBConfig.NAMESPACE)
}
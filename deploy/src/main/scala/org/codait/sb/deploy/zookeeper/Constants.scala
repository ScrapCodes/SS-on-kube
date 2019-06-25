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

object Constants {
  val ZK_POD_NAME = "zk"
  val ZK_IMAGE = "k8s.gcr.io/kubernetes-zookeeper:1.0-3.4.10"
  val ZK_CLIENT_PORT = 2181
  val ZK_SERVER_PORT = 2888
  val ZK_ELECTION_PORT = 3888
}

private[zookeeper] object Helpers {
  def zkContainerName(prefix: String)= s"${prefix}kubernetes-zookeeper"
  def zkStatefulSetName(prefix: String) = s"${prefix}zk"
  def zkClientServiceName(prefix: String) = s"${prefix}zk-cs"
  def zkInternalServiceName(prefix: String) = s"${prefix}zk-hs"
}
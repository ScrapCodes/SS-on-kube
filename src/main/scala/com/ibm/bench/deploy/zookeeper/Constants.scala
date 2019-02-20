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

import com.ibm.bench.util.SBConfig

object Constants {
  val ZK_POD_NAME = "zk"
  val ZK_CONTAINER_NAME = s"${SBConfig.PREFIX}kubernetes-zookeeper"
  val ZK_STATEFUL_SET_NAME = s"${SBConfig.PREFIX}zk"
  val ZK_IMAGE = "k8s.gcr.io/kubernetes-zookeeper:1.0-3.4.10"
  val ZK_CLIENT_PORT = 2181
  val ZK_SERVER_PORT = 2888
  val ZK_ELECTION_PORT = 3888
  val ZK_CLIENT_SERVICE_NAME = s"${SBConfig.PREFIX}zk-cs"
  val ZK_INTERNAL_SERVICE_NAME = s"${SBConfig.PREFIX}zk-hs"
}

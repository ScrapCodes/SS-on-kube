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

import org.codait.sb.deploy.ClusterConfig

case class ZKClusterConfig(clusterPrefix: String,
                           replicaSize: Int,
                           startTimeoutSeconds: Int,
                           override val kubernetesNamespace: String,
                           override val serviceAccount: String)
  extends ClusterConfig

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

import org.codait.sb.deploy.ClusterConfig

case class KafkaClusterConfig(override val clusterPrefix: String,
                              override val replicaSize: Int,
                              zookeeperAddress: String)
  extends ClusterConfig(clusterPrefix, replicaSize)
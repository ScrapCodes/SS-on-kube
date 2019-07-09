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

package org.codait.sb.deploy.spark

import org.codait.sb.deploy.ClusterConfig

case class SparkJobClusterConfig(name: String,
                                 masterUrl: String,
                                 sparkDeployMode: String,
                                 className: String,
                                 sparkImage: String,
                                 pathToJar: String,
                                 numberOfExecutors: Int,
                                 configParams: Map[String, String],
                                 sparkHome: Option[String] = None,
                                 packages: Seq[String],
                                 commandArgs: Seq[String],
                                 sparkDriverPort: Int = 38888,
                                 sparkBlockManagerPort: Int = 38889,
                                 override val kubernetesNamespace: String,
                                 override val serviceAccount: String)
  extends ClusterConfig
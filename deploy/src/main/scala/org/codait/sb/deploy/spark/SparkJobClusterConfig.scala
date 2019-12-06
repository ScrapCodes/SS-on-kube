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
                                 configParams: Map[String, String] = Map(),
                                 sparkHome: Option[String] = None,
                                 packages: Seq[String] = Seq(),
                                 commandArgs: Seq[String] = Seq(),
                                 sparkDriverPort: Int = 38888,
                                 imagePullPolicy: String = "Always",
                                 sparkBlockManagerPort: Int = 38889,
                                 override val kubernetesNamespace: String,
                                 override val serviceAccount: String)
  extends ClusterConfig {
}
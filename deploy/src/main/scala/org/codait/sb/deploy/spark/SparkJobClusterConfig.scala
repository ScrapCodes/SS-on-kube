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
                                 deployMode: String,
                                 className: String,
                                 sparkImage: String,
                                 kubeServiceAccount: String,
                                 pathToJar: String,
                                 numberOfExecutors: Int,
                                 configParams: Map[String, String],
                                 sparkHome: String,
                                 packages: Seq[String],
                                 commandArgs: Seq[String],
                                 override val kubernetesNamespace: String)
  extends ClusterConfig
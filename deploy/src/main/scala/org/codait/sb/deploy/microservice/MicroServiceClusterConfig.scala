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

package org.codait.sb.deploy.microservice

import org.codait.sb.deploy.ClusterConfig

case class MicroServiceClusterConfig(clusterPrefix: String,
                                     clusterName: String,
                                     initialReplicaSize: Int = 1,
                                     enableHorizontalPodAutoscaler: Boolean = false,
                                     microServiceImage: String,
                                     containerCommand: Option[Seq[String]] = None,
                                     extraLabels: Map[String, String] = Map.empty[String, String],
                                     namedServicePorts: Map[String, Int] = Map.empty[String, Int],
                                     envVars: Map[String, String] = Map.empty[String, String],
                                     startTimeoutSeconds: Int = 10,
                                     userContainerFeatures: Seq[AddContainerFeature] = Seq.empty,
                                     userServiceFeatures: Seq[AddServiceFeature] = Seq.empty,
                                     override val kubernetesNamespace: String = "default",
                                     override val serviceAccount: String) extends ClusterConfig
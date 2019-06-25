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

import java.util.UUID

import org.codait.sb.util.{SBConfig, ClusterUtils}
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.scalatest.concurrent.Eventually._
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.duration._

class ZkSanityTest extends FunSuite with BeforeAndAfterAll {

  private lazy val kubernetesClient = new DefaultKubernetesClient()
    .inNamespace(SBConfig.NAMESPACE)

  private val testingPrefix = s"t${UUID.randomUUID().toString.takeRight(5)}"

  private val zkCluster = new ZKCluster(ZKClusterConfig(clusterPrefix = testingPrefix,
    replicaSize = 3))

  override def beforeAll() {
    zkCluster.start()
    kubernetesClient
    assert(zkCluster.isRunning(20))
  }


  test("Zookeeper pods are up and initialized properly.") {
    val zkPods = zkCluster.getPods
    for (pod <- zkPods) {
      val podName = pod.getMetadata.getName

      val (_, podOrdinal) = podName.splitAt(podName.indexOf('-') + 1)
      eventually(timeout(2.minutes), interval(20.seconds)) {
        val (result: String, _) =
          ClusterUtils.execCommand(pod, command = "cat /var/lib/zookeeper/data/myid", kubernetesClient,
            chkResult = (podOrdinal.trim.toInt + 1).toString)

        assert(podOrdinal.trim.toInt + 1 == result.trim.toInt,
          "Incorrect zookeeper pod's Myid value.")
      }
    }
  }

  test("Zookeeper service is running and all the nodes have discovered each other.") {
    val zkPods = zkCluster.getPods
    val pod1 = zkPods.head
    val pod2 = zkPods.last

    // Clean up.
    ClusterUtils.execCommand(pod1, "zkCli.sh delete /hello", kubernetesClient)
    val createCommand = "zkCli.sh create /hello world"
    // Create an object, "Hello World".
    eventually(timeout(3.minutes), interval(30.seconds)) {
      val (result1: String, success: Boolean) =
        ClusterUtils.execCommand(pod1, command = createCommand, kubernetesClient, chkResult = "Created")
      assert(result1.contains("Created /hello"), s"Zookeeper object creation failed.$result1")
      // Retrieve same object from another pod.
      val (result2, success2) =
        ClusterUtils.execCommand(pod2, command = "zkCli.sh get /hello", kubernetesClient, chkResult = "world")
      assert(result2.contains("world"), s"Object could not be retrieved.$result2")
    }
  }

  override def afterAll(): Unit = {
    zkCluster.stop()
  }
}

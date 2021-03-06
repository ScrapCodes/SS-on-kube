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

package org.codait.sb.it.zookeeper

import org.codait.sb.it.{TestBase, TestSetup => ts}
import org.codait.sb.util.ClusterUtils
import org.scalatest.concurrent.Eventually._

import scala.concurrent.duration._

class ZkSanityTest extends TestBase {

  test("Zookeeper pods are up and initialized properly.") {
    val zkPods = ts.getZKCluster.getPods
    assert(zkPods.nonEmpty)

    for (pod <- zkPods; if zkPods.size > 1) { // Not valid test for single node Zk cluster.
      val podName = pod.getMetadata.getName

      val (_, podOrdinal) = podName.splitAt(podName.indexOf('-') + 1)
      eventually(timeout(2.minutes), interval(20.seconds)) {
        val (result: String, _) =
          ClusterUtils.execCommand(pod, command = "cat /var/lib/zookeeper/data/myid", ts.kubernetesClient,
            chkResult = (podOrdinal.trim.toInt + 1).toString)

        assert(podOrdinal.trim.toInt + 1 == result.trim.toInt,
          "Incorrect zookeeper pod's Myid value.")
      }
    }
  }

  test("Zookeeper service is running and all the nodes have discovered each other.") {
    val zkPods = ts.getZKCluster.getPods
    val pod1 = zkPods.head
    val pod2 = zkPods.last

    // Clean up.
    ClusterUtils.execCommand(pod1, "zkCli.sh delete /hello", ts.kubernetesClient)
    val createCommand = "zkCli.sh create /hello world"
    // Create an object, "Hello World" at zookeeper Node 1.
    eventually(timeout(3.minutes), interval(30.seconds)) {
      val (result1: String, success: Boolean) =
        ClusterUtils.execCommand(pod1, command = createCommand, ts.kubernetesClient, chkResult = "Created")
      assert(result1.contains("Created /hello"), s"Zookeeper object creation failed.$result1")
      // Retrieve same object from another pod or zookeeper node.
      val (result2, success2) =
        ClusterUtils.execCommand(pod2, command = "zkCli.sh get /hello", ts.kubernetesClient, chkResult = "world")
      assert(result2.contains("world"), s"Object could not be retrieved.$result2")
    }
  }

}

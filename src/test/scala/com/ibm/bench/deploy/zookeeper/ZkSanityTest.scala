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

import com.ibm.bench.deploy.SanityTestUtils
import com.ibm.bench.util.SBConfig
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.scalatest.concurrent.Eventually._
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class ZkSanityTest extends FunSuite with BeforeAndAfterAll {

  private lazy val kubernetesClient = new DefaultKubernetesClient()
    .inNamespace(SBConfig.NAMESPACE)

  override def beforeAll() {
    kubernetesClient
    val serviceList = kubernetesClient.services().list().getItems.asScala
    assert(serviceList.exists(_.getMetadata.getName ==
      Services.clientService.getMetadata.getName), "Cluster should be started before sanity tested.")
    assert(serviceList.exists(_.getMetadata.getName ==
      Services.internalService.getMetadata.getName), "Cluster should be started before sanity tested.")
    val ssName = ZKStatefulSet.statefulSet.getMetadata.getName
    val ss = kubernetesClient.apps().statefulSets().withName(ssName).get()
    SanityTestUtils.waitForClusterUpAndReady(kubernetesClient, ss, timeoutSeconds = 20)
  }


  test("Zookeeper pods are up and initialized properly.") {
    val zkPods = SanityTestUtils.getPodsWhenReady(kubernetesClient, Services.labels)
    for (pod <- zkPods) {
      val podName = pod.getMetadata.getName

      val (_, podOrdinal) = podName.splitAt(podName.indexOf('-') + 1)
      eventually(timeout(2.minutes), interval(20.seconds)) {
        val (result: String, _) =
          SanityTestUtils.execCommand(pod, command = "cat /var/lib/zookeeper/data/myid", kubernetesClient,
            chkResult = (podOrdinal.trim.toInt + 1).toString)

        assert(podOrdinal.trim.toInt + 1 == result.trim.toInt,
          "Incorrect zookeeper pod's Myid value.")
      }
    }
  }

  test("Zookeeper service is running and all the nodes have discovered each other.") {
    val zkPods = SanityTestUtils.getPodsWhenReady(kubernetesClient, Services.labels)
    val pod1 = zkPods.head
    val pod2 = zkPods.last

    // Clean up.
    SanityTestUtils.execCommand(pod1, "zkCli.sh delete /hello", kubernetesClient)
    val createCommand = "zkCli.sh create /hello world"
    // Create an object, "Hello World".
    eventually(timeout(3.minutes), interval(30.seconds)) {
      val (result1: String, success: Boolean) =
        SanityTestUtils.execCommand(pod1, command = createCommand, kubernetesClient, chkResult = "Created")
      assert(result1.contains("Created /hello"), s"Zookeeper object creation failed.$result1")
      // Retrieve same object from another pod.
      val (result2, success2) =
        SanityTestUtils.execCommand(pod2, command = "zkCli.sh get /hello", kubernetesClient, chkResult = "world")
      assert(result2.contains("world"), s"Object could not be retrieved.$result2")
    }
  }
}

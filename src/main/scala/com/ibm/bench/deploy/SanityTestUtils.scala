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

package com.ibm.bench.deploy

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.client.KubernetesClient
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

/** Sanity test a zookeeper cluster running inside the kubernetes cluster */
object SanityTestUtils {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def isStatefulSetReady(ss: StatefulSet): Boolean = {
    assert(ss != null, "StatefulSet can not be null")
    val spec = ss.getSpec
    val status = ss.getStatus
    if (status == null || status.getReplicas == null
      || status.getReadyReplicas == null || spec == null || spec.getReplicas == null) {
      false
    } else spec.getReplicas.intValue() == status.getReadyReplicas
  }

  /**
    *
    * @param ss Statefulset to be waited upon.
    * @param timeoutSeconds timeout in seconds.
    */
  def waitForClusterUpAndReady(client: KubernetesClient, ss: StatefulSet, timeoutSeconds: Int = 5): Unit = {
    val count = 20
    val sleep = timeoutSeconds * 1000 / count
    val ssName = ss.getMetadata.getName
    def ss2: StatefulSet = client.apps().statefulSets().withName(ssName).get()
    reAttempt(count = count, sleep = sleep, condition = () => !isStatefulSetReady(ss2),
      msg = () => s"Value of Spec: ${ss2.getSpec}, ${ss2.getStatus}")
  }

  def execCommand(pod: Pod,
                  command: String,
                  client: KubernetesClient,
                  chkResult: String = "",
                  reExecute: Boolean = false): (String, Boolean) = {

    val podName = pod.getMetadata.getName

    val baosOut = new ByteArrayOutputStream()
    val baosErr = new ByteArrayOutputStream()
    try {
      val execWatch = client.pods()
        .withName(podName)
        .writingOutput(baosOut)
        .writingError(baosErr)
        .exec("sh", "-c", command)

      def toString(b: Array[Byte]) = new String(b, Charset.forName("UTF-8"))

      def result = toString(baosErr.toByteArray) + toString(baosOut.toByteArray)

      val success = reAttempt(condition = () => baosOut.size() < 1 || !result.contains(chkResult),
        sleep = 1000,
        msg = () =>
          s"""Searching for string "$chkResult" in the output:
             | $result.""".stripMargin,
        throwException = false)

      (result, success)
    } finally {
      baosErr.close()
      baosOut.close()
    }
  }

  private[bench] def getPodsWhenReady(client: KubernetesClient,
                               labels: util.Map[String, String]): Seq[Pod] = {
    def podList: Seq[Pod] = client.pods().withLabels(labels).list().getItems.asScala

    reAttempt(condition = () => podList == null || podList.isEmpty)

    def podsPhases(): Seq[String] = podList.map(_.getStatus.getPhase)
    // Keep waiting until the pods are in Running phase.
    reAttempt(condition = () => null == podsPhases() ||
      !podsPhases().forall(_.equalsIgnoreCase("Running")),
      msg = () => s"all pods have transitioned to state Running, currently ${podsPhases()}"
    )
    podList
  }


  /**
    * Re attempt to evaluate the condition, until the count of number of tries finishes or condition
    * is successful.
    *
    * @param count     Number of retries, default is 10.
    * @param condition Condition to evaluate, until true.
    * @param sleep     milliseconds to sleep, between reattempts.
    * @param msg       Function to evaluate, for generating helpful debug messages.
    * @param throwException Whether to throw exception, helpful, in externally controlling reattempt.
    * @return
    */
  private def reAttempt(count: Int = 10,
                        condition: () => Boolean,
                        sleep: Int = 500,
                        msg: () => String = () => "",
                        throwException: Boolean = true): Boolean = {
    var i = count
    while (i > 0 && condition()) {
      Thread.sleep(sleep)
      logger.trace(msg())
      i = i - 1
    }

    if (i == 0 && throwException) {
      throw new BenchmarkException(
        s"Timed out trying to wait for the condition: \n ${msg()}\n")
    }
    logger.debug(s"Timed out trying to wait for the condition: \n ${msg()}\n")
    !(i == 0)
  }
}

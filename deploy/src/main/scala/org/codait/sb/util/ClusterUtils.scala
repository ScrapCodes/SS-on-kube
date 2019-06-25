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

package org.codait.sb.util

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.client.KubernetesClient
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

/** Utilities for cluster running inside the kubernetes */
private[sb] object ClusterUtils {

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
    * @param ss             Statefulset to wait for.
    * @param timeoutSeconds timeout in seconds.
    */
  def waitForClusterUpAndReady(
                                client: KubernetesClient,
                                ss: StatefulSet,
                                timeoutSeconds: Int = 5,
                                throwException: Boolean = true): Boolean = {
    val count = 20
    val ssName = ss.getMetadata.getName

    def ss2: StatefulSet = client.apps().statefulSets().withName(ssName).get()

    reAttempt(count = count, timeoutSeconds = timeoutSeconds, condition = () => isStatefulSetReady(ss2),
      msg = () => s"Value of Spec: ${ss2.getSpec}, ${ss2.getStatus}", throwException = throwException)
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

      val success = reAttempt(condition = () => baosOut.size() > 0 || result.contains(chkResult),
        timeoutSeconds = 10,
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

  def getPodsWhenReady(client: KubernetesClient,
                       labels: util.Map[String, String]): Seq[Pod] = {
    def podList: Seq[Pod] = client.pods().withLabels(labels).list().getItems.asScala

    reAttempt(condition = () => podList != null || podList.nonEmpty)

    def podsPhases(): Seq[String] = podList.map(_.getStatus.getPhase)
    // Keep waiting until the pods are in Running phase.
    reAttempt(condition = () => null != podsPhases() ||
      podsPhases().forall(_.equalsIgnoreCase("Running")),
      msg = () => s"all pods have not transitioned to state Running, currently ${podsPhases()}"
    )
    logger.debug(s"Pods: ${podList.map(_.getMetadata.getName).mkString(",")}, are running.")
    podList
  }


  /**
    * Re attempt to evaluate the condition, until the count of number of tries finishes or condition
    * is successful.
    *
    * @param count          Number of retries, default is 10.
    * @param condition      Condition to evaluate, until true.
    * @param timeoutSeconds total timeout to wait for.
    * @param msg            Function to evaluate, for generating helpful debug messages.
    * @param throwException Whether to throw exception, helpful, in externally controlling reattempt.
    * @return
    */
  def reAttempt(count: Int = 10,
                condition: () => Boolean,
                timeoutSeconds: Int = 5,
                msg: () => String = () => "",
                throwException: Boolean = true): Boolean = {
    var i = count
    val sleep = timeoutSeconds * 1000 / count
    while (i > 0 && !condition()) {
      Thread.sleep(sleep)
      logger.trace(msg())
      i = i - 1
    }

    if (i == 0 && throwException) {
      throw new DeploymentException(
        s"Timed out trying to wait for the condition: \n ${msg()}\n")
    }
    logger.debug(s"Timed out trying to wait for the condition: \n ${msg()}\n")
    !(i == 0)
  }
}

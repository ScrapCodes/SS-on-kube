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

package org.codait.sb.it.microservice

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, _}
import org.codait.sb.deploy.microservice.{MicroServiceCluster, MicroServiceClusterConfig}
import org.codait.sb.it.{TestBase, TestSetup => ts}
import org.scalatest.concurrent.Eventually.{eventually, interval, timeout}

import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success}

class MicroServiceSuite extends TestBase(false) {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))
  private var cluster: MicroServiceCluster = _

  test("Evaluate a ML model from IBM MAX repo.") {
    val portName = "rest"
    val config = MicroServiceClusterConfig(
      clusterPrefix = ts.testingPrefix,
      clusterName = "testing",
      enableHorizontalPodAutoscaler = false, // required for kubernetes v1.12.x
      microServiceImage = "codait/max-text-sentiment-classifier",
      namedServicePorts = Map(portName -> 5000),
      serviceAccount = serviceAccount
    )
    cluster = new MicroServiceCluster(config)
    cluster.start()

    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    val data =
      """
        |{
        |   "text" : [ "the mode evaluates the sentiment of a text."
        |   ]
        |}
      """.stripMargin

    def makeHttpPostReq(hostPort: String) = HttpRequest(
      method = HttpMethods.POST,
      uri = s"http://$hostPort/model/predict",
      entity = HttpEntity(ContentTypes.`application/json`, data)
    )

    eventually(timeout(2.minutes), interval(30.seconds)) {
      assert(cluster.isRunning(1), "Cluster should be running before we can test it.")
      val serviceAddress = cluster.serviceAddresses.head.externalAddress.get.toString
      logger.info("service addr: " + serviceAddress)
      val responseFuture = Http().singleRequest(makeHttpPostReq(serviceAddress))

      responseFuture.onComplete {
        case Success(HttpResponse(StatusCodes.OK, _, _, _)) =>

        case Failure(exception) => throw new AssertionError(exception)
      }
      Await.ready(responseFuture, 10.seconds)
      assert(responseFuture.value.get.isSuccess)
    }
  }

  override def afterAll(): Unit = {
    if (cluster != null) {
      cluster.stop()
    }
    super.afterAll()
  }
}

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

package org.codait.sb.deploy

import org.codait.sb.deploy.kafka.KafkaCluster
import org.codait.sb.deploy.zookeeper.ZKCluster
import org.slf4j.{Logger, LoggerFactory}

object Main {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def help(): String = {
    """
      |Currently not Implemented.
      |Help
      |
      |Supported commands:
      |
      |zk-start : Start the zookeeper cluster with default configurations.
      |zk-stop: Stop the zookeeper cluster started previously by zk-start command.
      |kafka-start: Start the kafka cluster.
      |Kafka-stop: Stop the kafka cluster started previously by kafka-start command.
    """.stripMargin
  }

  def main(args: Array[String]): Unit = {
    args(0) match {
//      case "zk-start" => ZKCluster.start()
//      case "zk-stop" => ZKCluster.stop()
//      case "kafka-start" => KafkaCluster.start()
//      case "kafka-stop" => KafkaCluster.stop()
      case _ => logger.info(help())
    }

  }

}

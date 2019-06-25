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

package org.codait.sb.bench

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.streaming.{StreamingQuery, Trigger}

/* Input record carries the timestamp of the time record was created and sent to kafka, and at
 * time of map only processing, we measure latency of messages. i.e. time that it took for spark
 * to read the message from kafka and write it out to another topic of kafka.
  *
  * Since this is map only, it can be run both in continuous and batch processing modes.*/
object SimpleMapOnlyBench {

  def benchmarkRunner(): Unit = {
    val df = BenchmarkUtils.streamRandomPayload()
    BenchmarkUtils.writeToKafka(df, BenchmarkUtils.bootStrapServer, "test1")
  }

  def main(args: Array[String]): Unit = {
    benchmarkRunner()
  }
}
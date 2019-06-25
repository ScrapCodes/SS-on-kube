package org.codait.sb.bench

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.streaming.{StreamingQuery, Trigger}

object BenchmarkUtils {
  // TODO: Make it configurable.
  val bootStrapServer: String = "kafka-service:9092"

  def readStreamFromKafka(jobName: String, bootStrapServer: String, kafkaTopic: String): DataFrame = {
    val spark = SparkSession.builder().appName(jobName).getOrCreate()
    spark.readStream.format("kafka")
      .option("kafka.bootstrap.servers", bootStrapServer)
      .option("subscribe", kafkaTopic)
      .load()
      .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
  }

  def writeToKafka(df: DataFrame, bootStrapServer: String, topic: String): StreamingQuery = {
    assert(df.isStreaming, "Supported kafka streaming only")
    df.writeStream.format("kafka")
      .option("kafka.bootstrap.servers", bootStrapServer)
      .option("topic", topic).trigger(Trigger.Continuous("1 second"))
      .start()
  }

  /* Random data generator with timestamp information.
   */
  def streamRandomPayload(): DataFrame = {
    val spark = SparkSession.builder().appName("DataGeneratorRandomPayload").getOrCreate()
    // USe the rate source to generate key and value, where timestamp is the key and value is int
    val df: DataFrame = spark.readStream.format("rate").option("rampUpTime", "30s")
      .option("numPartitions", "10").load()
      .selectExpr("CAST(timestamp AS STRING) AS key", "CAST(value AS STRING)")
    df
  }
}

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

package org.codait.sb.demo

import java.util.UUID

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

import scala.collection.mutable
import scala.io.Source

/**
  * Publish randomly selected tweets to a kafka topic.
  */
object SparkStreamingDataGenerator {
  def main(args: Array[String]): Unit = {
    val inputTweetStream = this.getClass.getClassLoader.getResourceAsStream("Tweets.csv")
    val tweetLines = Source.fromInputStream(inputTweetStream).getLines()
    val map = mutable.HashMap[Long, String]()
    var i = 0
    for (tweet <- tweetLines) {
      val splittedTweet: Array[String] = tweet.split(",")
      if (splittedTweet.length > 10) {
        val tweetText = splittedTweet(10)
        map.put(i, tweetText)
        i = i + 1
      }
    }
    val immutableMap = Map(map.iterator.toSeq: _*)
    val tweetCount = i
    // Spark
    val spark = SparkSession.builder.appName("Data Generator").getOrCreate()
    import spark.implicits._
    val broadcastTweetDataMap = spark.sparkContext.broadcast(immutableMap)
    val getTweet = udf((j: Long) => Seq(broadcastTweetDataMap.value(j)))
    val getRandomTweet = udf((j: Long) =>
      Seq(broadcastTweetDataMap.value((scala.math.random * 10000000).toLong % tweetCount)))

    val tweetStream = spark.readStream.format("rate")
      .option("rowsPerSecond", "1")
      .option("numPartitions", "2")
      .option("rampUpTime", "1s")
      .load()
      .select(getRandomTweet('value % tweetCount) as 'text)

    val df = tweetStream.select(current_timestamp() as 'key,
      to_json(struct("*")) as 'value)
      .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", s"${args(0)}")
      .option("topic", s"${args(1)}")
      .option("checkpointLocation", s"/tmp/${UUID.randomUUID()}")
      .start()
    df.awaitTermination()
    df.stop()
  }
}

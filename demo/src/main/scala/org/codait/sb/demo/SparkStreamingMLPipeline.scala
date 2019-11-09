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

import scala.collection.JavaConverters._
import java.io.IOException

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.SparkSession
import org.slf4j.{Logger, LoggerFactory}

/**
  * 1. Load from kafka
  * 2. For each tweet evaluate sentiment by querying the MAX restful service.
  * 3. Print the results and statistics on console.
  */
object SparkStreamingMLPipeline {

  val JSON: MediaType = MediaType.parse("application/json; charset=utf-8")
  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  @throws[IOException]
  def post(url: String, json: String, client: OkHttpClient): String = {
    val body = RequestBody.create(JSON, json)
    val request = new Request.Builder().url(url).post(body).build
    val response = client.newCall(request).execute
    try {
      response.body.string()
    } finally {
      if (response != null) response.close()
    }
  }

  def parseResponseJson(json: String, parser: JSONParser): String = {
    try {
      val jsonObject: JSONObject = parser.parse(json).asInstanceOf[JSONObject]
      assert(jsonObject.get("status").asInstanceOf[String].equals("ok"))
      val predictions = jsonObject.get("predictions").asInstanceOf[JSONArray].iterator().asScala
      predictions.map { p =>
        val positivePob = p.asInstanceOf[JSONObject].get("positive").asInstanceOf[Double]
        val negativePob = p.asInstanceOf[JSONObject].get("negative").asInstanceOf[Double]
        if (negativePob - positivePob > 0.4) {
          "negative"
        } else if (positivePob - negativePob > 0.4) {
          "positive"
        } else {
          "neutral"
        }
      }.mkString(",")
    } catch {
      case e: Exception =>
        logger.error(s"The response json: $json, that caused the exception:", e)
        throw e
    }
  }

  def parseTweetJson(tweetJson: String, parser: JSONParser): String = {
    val jsonObject = parser.parse(tweetJson).asInstanceOf[JSONObject]
    val tweetText = jsonObject.get("text").asInstanceOf[JSONArray]
      .iterator().next().asInstanceOf[String]
    tweetText
  }

  def parseAirlineName(tweet: String): String = {
    val set = Set("southwestair",
      "united",
      "virginamerica",
      "jetblue",
      "delta",
      "usairways",
    "americanair")

    val regex = ".*?@(\\w+)\\s+.*".r
    val m = regex.pattern.matcher(tweet.replaceAll("\"", ""))
    if (m.find() && set.contains(m.group(1).toLowerCase)) {
      m.group(1)
    } else {
      "can't tell"
    }
  }

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.appName("SparkMLPipeline")
      .getOrCreate()
    import spark.implicits._
    val tweetDataset = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", s"${args(0)}")
      .option("subscribe", s"${args(1)}")
      .option("startingOffsets", "earliest")
      .option("checkpointLocation", s"/tmp/${UUID.randomUUID()}")
      .load()
      .selectExpr("CAST(value AS STRING)")
      .as[String]
    // TODO: We are querying each tweet one by one, we can speed up by
    // using bulk query instead.
    val tweetSentiment = tweetDataset.map { tweetJson =>
      val client = new OkHttpClient()
      val parser = new JSONParser()
      val resp = post(s"${args(2)}", tweetJson, client)
      (parseTweetJson(tweetJson, parser), parseResponseJson(resp, parser))
    }

    val tweetStats =
      tweetSentiment.map(x => (parseAirlineName(x._1).toLowerCase, x._1, x._2))
        .toDF("airline", "tweet", "sentiment")
        .groupBy('airline, 'sentiment).count()

    val df = tweetSentiment.toDF("tweet", "sentiment")
      .writeStream
      .trigger(Trigger.Continuous("5 seconds"))
      .format("console")
      .option("checkpointLocation", s"/tmp/${UUID.randomUUID()}")
      .option("truncate", "false")
      .start()

    val df2 = tweetStats
      .writeStream
      .trigger(Trigger.ProcessingTime("12 seconds"))
      .outputMode(OutputMode.Update())
      .option("checkpointLocation", s"/tmp/${UUID.randomUUID()}")
      .format("console")
      .option("truncate", "false")
      .start()
    df.awaitTermination()
    df2.awaitTermination()
    df.stop()
    df2.stop()
  }
}

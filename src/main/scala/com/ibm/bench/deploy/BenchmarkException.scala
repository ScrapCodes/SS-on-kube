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

case class BenchmarkException(message: String, cause: Throwable)
  extends Exception(message, cause) {
  def this(message: String) {
    this(message, null)
  }
}

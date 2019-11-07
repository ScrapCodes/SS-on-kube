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

package org.codait.sb.it

import org.codait.sb.it.{TestSetup => ts}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class TestBase(kafka: Boolean = true) extends FunSuite with BeforeAndAfterAll {
  override def beforeAll(): Unit = {
    super.beforeAll()
    if (kafka) {
      ts.init()
    }
  }
}

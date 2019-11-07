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

case class Address(host: String, port: Int) {
  override def toString: String = {
    s"$host:$port"
  }
}

case class ServiceAddresses(internalAddress: Option[Address] = None,
                            externalAddress: Option[Address] = None)

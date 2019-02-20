package com.ibm.bench.util

object SBConfig {

//  val loadProperties = Thread.currentThread().getContextClassLoader
//    .getResourceAsStream("sb.properties")

  val NAMESPACE: String = System.getProperty("sb.kubernetes.namespace", "default")

  val PREFIX: String = System.getProperty("sb.kubernetes.name-prefix", "")
}

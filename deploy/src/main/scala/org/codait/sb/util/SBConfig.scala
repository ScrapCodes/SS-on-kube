package org.codait.sb.util

object SBConfig {

  val NAMESPACE: String = System.getProperty("sb.kubernetes.namespace", "default")

  val PREFIX: String = System.getProperty("sb.kubernetes.name-prefix", "")
}

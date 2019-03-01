package org.codait.sb.util

object SBConfig {
  def masterUrl: String = System.getProperty("sb.kubernetes.master")

  val NAMESPACE: String = System.getProperty("sb.kubernetes.namespace", "default")

  val PREFIX: String = System.getProperty("sb.kubernetes.name-prefix", "")
}

package com

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.text.DecimalFormat

private const val K: Long = 1024
private const val M = K * K
private const val G = M * K
private const val T = G * K

fun File.prettySize(): String? {
  val value = length()
  val dividers = longArrayOf(T, G, M, K, 1)
  val units = arrayOf("TB", "GB", "MB", "KB", "B")
  require(value >= 0) { "Invalid file size: $value" }
  var result: String? = null
  for (i in dividers.indices) {
    val divider = dividers[i]
    if (value >= divider) {
      result = format(value, divider, units[i])
      break
    }
  }
  return result
}

private fun format(
  value: Long,
  divider: Long,
  unit: String
): String {
  val result = if (divider > 1) value.toDouble() / divider.toDouble() else value.toDouble()
  return DecimalFormat("#,##0.#").format(result) + " " + unit
}

fun initStorage() {
  val dbDir = FileSystems.getDefault().getPath("uploads");
  try {
    Files.createDirectories(dbDir)
  } catch (e: UnsupportedOperationException) {}
}
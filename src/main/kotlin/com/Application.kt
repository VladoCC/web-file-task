package com

import io.ktor.application.*
import com.plugins.*
import java.nio.file.FileSystems
import java.nio.file.Files

fun main(args: Array<String>): Unit =
  io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
  initStorage()
  configureRouting()
  configureSerialization()
}
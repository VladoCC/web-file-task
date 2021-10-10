package com.plugins

import com.prettySize
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.request.*
import java.io.File

fun Application.configureRouting() {
  routing {
    route("files") {
      get {
        val files = File("uploads").listFiles()?.associate { it.name to it.prettySize() } ?: emptyMap()
        call.respond(mapOf("files" to files))
      }
      post {
        val multipartData = call.receiveMultipart()
        var fileName = ""
        var success = false
        multipartData.forEachPart { part ->
          when (part) {
            is PartData.FileItem -> {
              fileName = part.originalFileName as String
              val file = File("uploads/$fileName")
              if (fileName.isNotEmpty() && !file.exists()) {
                val fileBytes = part.streamProvider().readBytes()
                file.writeBytes(fileBytes)
                success = true
              }
            }
            else -> {}
          }
        }

        if (success) {
          call.respond(HttpStatusCode.OK, "$fileName uploaded")
        } else if (File("uploads/$fileName").exists()) {
          call.respond(HttpStatusCode.BadRequest, "File with name $fileName already exists")
        } else {
          call.respond(HttpStatusCode.BadRequest, Unit)
        }
      }
      route("/{name}") {
        get {
          val name = call.parameters["name"]?: ""

          val file = File("uploads/$name")
          if (name.isNotEmpty() && file.exists()) {
            call.response.header(
              HttpHeaders.ContentDisposition,
              ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, name)
                .toString()
            )
            call.respondFile(file)
          } else {
            call.respond(HttpStatusCode.BadRequest, Unit)
          }
        }
        delete {
          val name = call.parameters["name"]?: ""

          val file = File("uploads/$name")
          if (name.isNotEmpty() && file.exists()) {
            file.delete()
            call.respond(HttpStatusCode.OK, "$name deleted")
          } else {
            call.respond(HttpStatusCode.BadRequest, "$name doesn't exist")
          }
        }
      }
    }
  }
}


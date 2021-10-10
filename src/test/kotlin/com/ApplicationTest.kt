package com

import com.google.gson.Gson
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import kotlin.test.*
import io.ktor.server.testing.*
import com.plugins.*
import io.ktor.http.content.*
import io.ktor.utils.io.streams.*
import java.io.File

class ApplicationTest {
  data class GetFilesResponse(val files: Map<String, String>)

  private val gson = Gson()
  private fun Gson.getFilesResponse(json: String) = fromJson(json, GetFilesResponse::class.java)

  private fun TestApplicationEngine.uploadFile(filename: String, text: String, block: TestApplicationCall.() -> Unit) {
    with(handleRequest(HttpMethod.Post, "/files") {
      val boundary = "WebAppBoundary"
      val fileBytes = text.toByteArray()

      addHeader(HttpHeaders.ContentType, ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString())
      setBody(boundary, listOf(
        PartData.FileItem({ fileBytes.inputStream().asInput() }, {}, headersOf(
          HttpHeaders.ContentDisposition,
          ContentDisposition.File
            .withParameter(ContentDisposition.Parameters.Name, "text")
            .withParameter(ContentDisposition.Parameters.FileName, "text.txt")
            .toString()
        ))
      ))
    }) {
      block()
    }
  }

  @Test
  fun testCreateReadDelete() {
    withTestApplication({ module() }) {
      with(handleRequest(HttpMethod.Get, "/files")) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertNotNull(response.content)
        assert(gson.getFilesResponse(response.content!!).files.isEmpty())
      }

      val text = "Test text"
      uploadFile("text.txt", text) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals("text.txt uploaded", response.content)
      }

      uploadFile("text.txt", text) {
        assertEquals(HttpStatusCode.BadRequest, response.status())
        assertEquals("File with name text.txt already exists", response.content)
      }

      uploadFile("", text) {
        assertEquals(HttpStatusCode.BadRequest, response.status())
      }

      with(handleRequest(HttpMethod.Get, "/files")) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertNotNull(response.content)
        val response = gson.getFilesResponse(response.content!!)
        assert(response.files.size == 1)
        assert(response.files.containsKey("text.txt"))
        assertEquals("9 B", response.files["text.txt"])
      }

      with(handleRequest(HttpMethod.Get, "/files/text.txt")) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertNotNull(response.content)
        assertEquals(text, response.content)
      }

      with(handleRequest(HttpMethod.Get, "/files/other.txt")) {
        assertEquals(HttpStatusCode.BadRequest, response.status())
      }

      with(handleRequest(HttpMethod.Delete, "/files/text.txt")) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals("text.txt deleted", response.content)
      }

      with(handleRequest(HttpMethod.Delete, "/files/other.txt")) {
        assertEquals(HttpStatusCode.BadRequest, response.status())
        assertEquals("other.txt doesn't exist", response.content)
      }

      with(handleRequest(HttpMethod.Get, "/files")) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertNotNull(response.content)
        assert(gson.getFilesResponse(response.content!!).files.isEmpty())
      }
    }
  }
}
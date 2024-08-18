package org.godker.http.server

import io.ktor.network.sockets.*
import kotlinx.coroutines.delay
import org.godker.http.server.message.*

class Client {
    suspend fun processClient(clientSocket: Socket) {
        println("Accepted connection from ${clientSocket.remoteAddress}")

        clientSocket.use {

            val reader = clientSocket.openReadChannel()
            val writer = clientSocket.openWriteChannel(true)

            try {
                while (true) {
                    val request = HttpRequest.fromStream(reader) ?: break

                    println("Received a ${request.method} ${request.path}")

                    val response = processRequest(request)

                    response.serializeToStream(writer)

                    delay(5)
                }
                clientSocket.close()

            } catch (e: Throwable) {
                clientSocket.close()
            }
        }
    }

    private fun processRequest(request: HttpRequest): HttpResponse {
        when (request.method) {
            HttpMethod.GET -> return processGet(request)
            else -> throw NotImplementedError()
        }
    }

    private fun processGet(request: HttpRequest): HttpResponse {
        val requestedFile = if (request.path == "/") "index.html" else request.path

        val requestedFileStream = try {
            object {}.javaClass.getResourceAsStream("/webpage/${requestedFile}")!!
        } catch (e: Exception) {
            return HttpResponse(status = HttpStatus.NOT_FOUND)
        }

        val fileByteArray = requestedFileStream.readAllBytes()

        //TODO process other file formats (.ico, etc)
        val headers = mutableListOf<HttpHeader>()

        headers.add(HttpHeader("Content-Type", "text-html; charset=utf-8"))
        headers.add(HttpHeader("Content-Length", "${fileByteArray.size}"))
        headers.add(HttpHeader("Connection", "keep-alive"))
        headers.add(HttpHeader("Cache-Control", "no-cache"))

        return HttpResponse(status = HttpStatus.OK, headers = headers, body = fileByteArray.decodeToString())
    }
}
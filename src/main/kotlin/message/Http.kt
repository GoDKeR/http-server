package org.godker.http.server.message

import io.ktor.util.cio.*
import io.ktor.utils.io.*

enum class HttpMethod {
    CONNECT, DELETE, GET, HEAD, OPTIONS, POST, PUT;

    companion object {
        val methods by lazy { HttpMethod.entries.map { it.name } }
    }
}

enum class HttpStatus(val statusCode: Int, val statusText: String){
    CONTINUE(100, "Continue"),
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),
    PROCESSING(102, "Processing"),
    EARLY_HINTS(103, "Early Hints"),
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
    NO_CONTENT(204, "No Content"),
    RESET_CONTENT(205, "Reset Content"),
    PARTIAL_CONTENT(206, "Partial Content"),
    MULTI_STATUS(207, "Multi-Status"),
    ALREADY_REPORTED(208, "Already Reported"),
    IM_USED(226, "IM Used"),
    MULTIPLE_CHOICES(300, "Multiple Choices"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    SEE_OTHER(303, "See Other"),
    NOT_MODIFIED(304, "Not Modified"),
    USE_PROXY_DEPRECATED(305, "Use Proxy Deprecated"),
    UNUSED(306, "unused"),
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    PERMANENT_REDIRECT(308, "Permanent Redirect"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    PAYMENT_REQUIRED_EXPERIMENTAL(402, "Payment Required Experimental"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    REQUEST_TIMEOUT(408, "Request Timeout"),
    CONFLICT(409, "Conflict"),
    GONE(410, "Gone"),
    LENGTH_REQUIRED(411, "Length Required"),
    PRECONDITION_FAILED(412, "Precondition Failed"),
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
    URI_TOO_LONG(414, "URI Too Long"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),
    EXPECTATION_FAILED(417, "Expectation Failed"),
    IM_A_TEAPOT(418, "Im a teapot"),
    MISDIRECTED_REQUEST(421, "Misdirected Request"),
    UNPROCESSABLE_CONTENT(422, "Unprocessable Content"),
    LOCKED(423, "Locked"),
    FAILED_DEPENDENCY(424, "Failed Dependency"),
    TOO_EARLY_EXPERIMENTAL(425, "Too Early Experimental"),
    UPGRADE_REQUIRED(426, "Upgrade Required"),
    PRECONDITION_REQUIRED(428, "Precondition Required"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),
    VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),
    INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
    LOOP_DETECTED(508, "Loop Detected"),
    NOT_EXTENDED(510, "Not Extended"),
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required"),
}

data class HttpHeader(val name: String, val value: String)

data class HttpRequest(
    var method: HttpMethod,
    var path: String,
    var headers: List<HttpHeader> = emptyList(),
    var body: String? = null
) {
    companion object {
        suspend fun fromStream(stream: ByteReadChannel): HttpRequest? {
            try {
                val (method, path, version) = stream.readUTF8Line()!!.split(" ")

                if (method !in HttpMethod.methods)
                    return null

                if (path.isEmpty())
                    return null

                var line: String?
                val headers = mutableListOf<HttpHeader>()

                while (stream.readUTF8Line().also { line = it } !in listOf("\r\n", "\n", "\r", "", null)) {
                    val (name, value) = line!!.split(":")

                    headers.add(HttpHeader(name, value))
                }

                if (HttpMethod.valueOf(method) in listOf(HttpMethod.PUT, HttpMethod.POST)){
                    val sb = StringBuilder()
                    line = ""

                    while (stream.readUTF8Line().also { line = it } !in listOf("", null)) {
                        sb.append(line)
                    }
                    return HttpRequest(HttpMethod.valueOf(method), path, headers, sb.toString())
                }

                return HttpRequest(HttpMethod.valueOf(method), path, headers)

            } catch (e: Exception) {
                return null
            }
        }
    }
}

data class HttpResponse(
    val protocol: String = "HTTP/1.1",
    val status: HttpStatus,
    val headers: List<HttpHeader> = emptyList(),
    val body: String? = null
) {
    suspend fun serializeToStream(stream: ByteWriteChannel) {
        stream.use {
            writeStringUtf8("$protocol ${status.statusCode} ${status.statusText}\n")

            if (headers.isEmpty())
                return@use

            for (header in headers) {
                writeStringUtf8("${header.name}: ${header.value}\n")
            }

            if (!body.isNullOrBlank()) {
                writeStringUtf8("\n$body")
            }
        }
    }
}

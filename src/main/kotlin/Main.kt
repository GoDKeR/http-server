package org.godker.http.server

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*


fun main() = runBlocking{
    val selectorManager = SelectorManager(Dispatchers.IO)
    val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", 7666)

    var isRunning = true

    coroutineScope {
        while (isRunning) {
            val socket = serverSocket.accept()

            launch {
                val client = Client().processClient(socket)
            }

            delay(5)
        }
    }
    serverSocket.close()
    selectorManager.close()

    println("Server stop.")
}
package it.solphivia.mobile.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

private fun requestFor(endpoint: String = "https://example.test/v1/responses"): FrozenChatRequest =
    RequestSnapshotFactory.freeze(
        messages = listOf(ConversationMessage("user", "synthetic prompt")),
        mode = RequestMode.TYPED,
        configuration = ProviderConfiguration(
            ProviderIdentity(ProviderId.OPENAI_COMPATIBLE, "Mock OpenAI"),
            endpoint,
            "mock-model"
        )
    )

private class QueueExecutor : Executor {
    var task: Runnable? = null
    override fun execute(command: Runnable) { task = command }
}

private class MockSseServer {
    private val server = ServerSocket(0)
    private val thread = Thread {
        server.accept().use { socket ->
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.ISO_8859_1))
            val headers = StringBuilder()
            var contentLength = 0
            while (true) {
                val line = input.readLine() ?: break
                if (line.isEmpty()) break
                headers.append(line).append('\n')
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(':').trim().toInt()
                }
            }
            val body = CharArray(contentLength)
            var offset = 0
            while (offset < body.size) offset += input.read(body, offset, body.size - offset)
            receivedBody = String(body)
            val responseBody = listOf(
                "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Hello \"}\n\n",
                "data: {\"type\":\"response.output_text.delta\",\"delta\":\"[[SOLPHIVIA:nod]]\"}\n\n",
                "data: {\"type\":\"response.completed\"}\n\n"
            ).joinToString("")
            val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
            writer.write("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nContent-Length: ${responseBody.toByteArray().size}\r\nConnection: close\r\n\r\n$responseBody")
            writer.flush()
        }
    }
    @Volatile var receivedBody: String = ""
    val endpoint: String get() = "http://127.0.0.1:${server.localPort}/v1/responses"
    fun start() { thread.start() }
    fun stop() { server.close(); thread.join(2_000) }
}

class OpenAiCompatibilityProviderTest {
    @Test
    fun duplicateTerminalEventsProduceOnlyOneOutcome() {
        val completed = AtomicInteger()
        val failed = AtomicInteger()
        val provider = OpenAiCompatibilityProvider(
            Executor { it.run() },
            OpenAiTransport { _, stream, _ ->
                stream.onDelta("Hello [[SOLPHIVIA:nod]]")
                stream.onCompleted("Hello [[SOLPHIVIA:nod]]")
                stream.onError(ProviderError(ProviderErrorCategory.PROVIDER_FAILURE, "late failure"))
            }
        )

        val handle = provider.generate(requestFor(), object : GenerationListener {
            override fun onDelta(text: String) = Unit
            override fun onCompleted(response: ProviderResponse) { completed.incrementAndGet() }
            override fun onError(error: ProviderError) { failed.incrementAndGet() }
        })

        assertEquals(1, completed.get())
        assertEquals(0, failed.get())
        assertTrue(!handle.isCancelled)
    }

    @Test
    fun cancellationBeforeConnectionPreventsTransportAndEmitsCancelledOnce() {
        val executor = QueueExecutor()
        val started = AtomicInteger()
        val errors = mutableListOf<ProviderError>()
        val provider = OpenAiCompatibilityProvider(
            executor,
            OpenAiTransport { _, _, _ -> started.incrementAndGet() }
        )

        val handle = provider.generate(requestFor(), object : GenerationListener {
            override fun onDelta(text: String) = Unit
            override fun onCompleted(response: ProviderResponse) { error("unexpected completion") }
            override fun onError(error: ProviderError) { errors += error }
        })

        assertTrue(handle.cancel())
        assertTrue(!handle.cancel())
        executor.task?.run()

        assertEquals(0, started.get())
        assertEquals(1, errors.size)
        assertEquals(ProviderErrorCategory.CANCELLED, errors.single().category)
    }

    @Test
    fun cancellationDuringStreamingSuppressesLateCompletion() {
        val started = CountDownLatch(1)
        val released = CountDownLatch(1)
        val completed = AtomicInteger()
        val errors = AtomicReference<ProviderError>()
        val provider = OpenAiCompatibilityProvider(
            Executors.newSingleThreadExecutor(),
            OpenAiTransport { _, stream, cancellation ->
                started.countDown()
                cancellation.onCancel { released.countDown() }
                stream.onDelta("partial")
                released.await(5, TimeUnit.SECONDS)
                stream.onCompleted("late complete")
            }
        )

        val handle = provider.generate(requestFor(), object : GenerationListener {
            override fun onDelta(text: String) = Unit
            override fun onCompleted(response: ProviderResponse) { completed.incrementAndGet() }
            override fun onError(error: ProviderError) { errors.set(error) }
        })
        assertTrue(started.await(5, TimeUnit.SECONDS))
        assertTrue(handle.cancel())
        Thread.sleep(50)

        assertEquals(0, completed.get())
        assertEquals(ProviderErrorCategory.CANCELLED, errors.get().category)
    }

    @Test
    fun mockServerSseResponseIsParsedThroughTheCompatibilityBoundary() {
        val server = MockSseServer()
        server.start()
        val executor = Executors.newSingleThreadExecutor()
        try {
            val endpoint = server.endpoint
            val provider = OpenAiCompatibilityProvider(
                executor,
                OpenAiTransport { request, stream, cancellation ->
                    val connection = URL(request.configuration.endpoint).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    cancellation.onCancel { connection.disconnect() }
                    connection.outputStream.use { it.write("{\"model\":\"${request.configuration.model}\"}".toByteArray()) }
                    connection.inputStream.bufferedReader().useLines { lines ->
                        val full = StringBuilder()
                        var completed = false
                        lines.forEach { line ->
                            if (!line.startsWith("data:")) return@forEach
                            val json = line.removePrefix("data:").trim()
                            if (json.contains("response.output_text.delta")) {
                                val delta = Regex("""\"delta\":\"(.*?)\"""")
                                    .find(json)?.groupValues?.get(1).orEmpty()
                                    .replace("\\\"", "\"")
                                full.append(delta)
                                stream.onDelta(delta)
                            } else if (json.contains("response.completed")) completed = true
                        }
                        if (completed) stream.onCompleted(full.toString())
                    }
                    connection.disconnect()
                }
            )
            val responseValue = AtomicReference<ProviderResponse>()
            val errorValue = AtomicReference<ProviderError>()
            val deltas = mutableListOf<String>()
            val done = CountDownLatch(1)
            provider.generate(requestFor(endpoint), object : GenerationListener {
                override fun onDelta(text: String) { deltas += text }
                override fun onCompleted(response: ProviderResponse) { responseValue.set(response); done.countDown() }
                override fun onError(error: ProviderError) { errorValue.set(error); done.countDown() }
            })

            assertTrue(done.await(5, TimeUnit.SECONDS))
            assertEquals(null, errorValue.get())
            assertEquals("Hello", responseValue.get().text)
            assertEquals("nod", responseValue.get().animation)
            assertEquals(listOf("Hello "), deltas)
            assertTrue(server.receivedBody.contains("mock-model"))
        } finally {
            executor.shutdownNow()
            server.stop()
        }
    }
}

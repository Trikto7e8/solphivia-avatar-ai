package it.solphivia.mobile.provider

import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

fun interface OpenAiTransport {
    @Throws(Exception::class)
    fun execute(
        request: FrozenChatRequest,
        listener: OpenAiStreamListener,
        cancellation: RequestCancellation
    )
}

/**
 * OpenAI-compatible boundary used by the legacy ChatGPT client and by deterministic tests.
 * It owns terminal delivery, strips avatar controls from deltas, and never falls back to another provider.
 */
class OpenAiCompatibilityProvider @JvmOverloads constructor(
    private val executor: Executor,
    private val transport: OpenAiTransport,
    private val declaredCapabilities: ProviderCapabilities = ProviderCapabilities(
        text = true,
        streaming = true,
        cancellation = true,
        images = true,
        documents = true,
        tools = false,
        maxAttachmentBytes = 15L * 1024L * 1024L
    )
) : ModelProvider {
    override val identity: ProviderIdentity = ProviderIdentity(
        ProviderId.OPENAI_COMPATIBLE,
        "OpenAI compatible"
    )

    override fun capabilities(configuration: ProviderConfiguration): ProviderCapabilities = declaredCapabilities

    override fun generate(request: FrozenChatRequest, listener: GenerationListener): RequestCancellation {
        val lifecycle = RequestLifecycle(listener)
        declaredCapabilities.validate(request)?.let {
            lifecycle.fail(it)
            return lifecycle
        }

        try {
            executor.execute {
                if (lifecycle.isCancelled) return@execute
                val rawText = StringBuilder()
                val filter = StreamingTagFilter()
                val streamListener = object : OpenAiStreamListener {
                    override fun onDelta(text: String) {
                        if (lifecycle.isCancelled) return
                        rawText.append(text)
                        lifecycle.onDelta(filter.feed(text))
                    }

                    override fun onCompleted(fullText: String) {
                        if (lifecycle.isCancelled) return
                        val complete = fullText.ifBlank { rawText.toString() }
                        if (complete.trim().isEmpty()) {
                            lifecycle.onError(
                                ProviderError(
                                    ProviderErrorCategory.MALFORMED_RESPONSE,
                                    "Il provider ha restituito una risposta vuota."
                                )
                            )
                            return
                        }
                        filter.finish()
                        lifecycle.onCompleted(complete)
                    }

                    override fun onError(error: ProviderError) = lifecycle.onError(error)
                }
                try {
                    transport.execute(request, streamListener, lifecycle)
                    if (!lifecycle.isTerminated) {
                        // A transport that returns without a terminal event is a broken/partial stream.
                        lifecycle.onError(
                            ProviderError(
                                ProviderErrorCategory.MALFORMED_RESPONSE,
                                "La connessione si è interrotta prima del completamento della risposta."
                            )
                        )
                    }
                } catch (error: Throwable) {
                    if (!lifecycle.isCancelled) lifecycle.onError(ProviderError.fromThrowable(error))
                }
            }
        } catch (_: RejectedExecutionException) {
            lifecycle.fail(
                ProviderError(
                    ProviderErrorCategory.PROVIDER_FAILURE,
                    "Il servizio del provider non è disponibile."
                )
            )
        }
        return lifecycle
    }
}

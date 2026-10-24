package it.solphivia.mobile.provider

import java.util.Locale

private const val TAG_PREFIX = "[[SOLPHIVIA:"

private val KNOWN_TAGS = setOf(
    "idle", "look", "happy", "concerned", "sad", "wave", "nod", "explain", "celebrate", "sit",
    "stay", "left", "right", "center", "up", "down", "near", "away", "bounce", "run-left", "run-right",
    "mode-prof", "mode-vj", "mode-codex", "mode-meteorina"
)

object SolphiviaResponseParser {
    @JvmStatic
    fun parse(raw: String): ProviderResponse {
        var animation = "idle"
        var motion = "stay"
        var mode = ""
        val cleaned = StringBuilder()
        var cursor = 0
        while (cursor < raw.length) {
            val start = raw.indexOf(TAG_PREFIX, cursor, ignoreCase = true)
            if (start < 0) {
                cleaned.append(raw, cursor, raw.length)
                break
            }
            cleaned.append(raw, cursor, start)
            val bodyStart = start + TAG_PREFIX.length
            val close = raw.indexOf(']', bodyStart)
            if (close < 0) break
            val tag = raw.substring(bodyStart, close).lowercase(Locale.ROOT)
            val complete = close + 1 < raw.length && raw[close + 1] == ']'
            if (complete) {
                when {
                    tag in setOf("idle", "look", "happy", "concerned", "sad", "wave", "nod", "explain", "celebrate", "sit") -> animation = tag
                    tag.startsWith("mode-") && tag in KNOWN_TAGS -> mode = tag.removePrefix("mode-")
                    tag in KNOWN_TAGS -> motion = tag
                }
                cursor = close + 2
            } else {
                // A control prefix reserves only its malformed fragment; ordinary text after
                // a single closing bracket remains visible.
                cursor = close + 1
            }
        }
        return ProviderResponse(
            text = cleaned.toString().trim().ifEmpty { "Sono qui con te." },
            animation = animation,
            motion = motion,
            mode = mode
        )
    }
}

/** Holds possible control tags until the complete tag is available. */
class StreamingTagFilter {
    private val candidate = StringBuilder()

    fun feed(delta: String): String {
        val output = StringBuilder()
        for (character in delta) {
            if (candidate.isEmpty()) {
                if (character == '[') candidate.append(character) else output.append(character)
            } else {
                candidate.append(character)
            }
            drain(output)
        }
        return output.toString()
    }

    /** Never exposes an incomplete or malformed control tag at end of stream. */
    fun finish(): String {
        val output = StringBuilder()
        if (candidate.isNotEmpty() && !candidate.toString().regionMatches(0, TAG_PREFIX, 0, TAG_PREFIX.length, ignoreCase = true)) {
            output.append(candidate)
        }
        candidate.clear()
        return output.toString()
    }

    private fun drain(output: StringBuilder) {
        while (candidate.isNotEmpty()) {
            val value = candidate.toString()
            if (value.length <= TAG_PREFIX.length && TAG_PREFIX.regionMatches(0, value, 0, value.length, ignoreCase = true)) return
            if (value.regionMatches(0, TAG_PREFIX, 0, TAG_PREFIX.length, ignoreCase = true)) {
                val close = value.indexOf(']', TAG_PREFIX.length)
                if (close < 0) return
                // Hold a trailing single bracket until the next chunk so a split `]]` remains
                // indistinguishable from a complete control.
                if (close == value.length - 1) return
                val end = if (value[close + 1] == ']') close + 2 else close + 1
                // Complete controls and malformed controls are both consumed; only text after a
                // malformed single bracket is allowed back into the visible stream.
                candidate.delete(0, end)
                continue
            }
            output.append(candidate[0])
            candidate.deleteCharAt(0)
        }
    }
}

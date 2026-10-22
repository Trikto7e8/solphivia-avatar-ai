package it.solphivia.mobile.provider

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
        val cleaned = Regex("\\[\\[SOLPHIVIA:([^]]*)]]", RegexOption.IGNORE_CASE).replace(raw) { match ->
            val tag = match.groupValues[1].lowercase()
            when {
                tag in setOf("idle", "look", "happy", "concerned", "sad", "wave", "nod", "explain", "celebrate", "sit") -> animation = tag
                tag.startsWith("mode-") && tag in KNOWN_TAGS -> mode = tag.removePrefix("mode-")
                tag in KNOWN_TAGS -> motion = tag
            }
            ""
        }
        val withoutMalformedTags = cleaned.replace(
            Regex("\\[\\[SOLPHIVIA:[^]]*(?:]]|$)", RegexOption.IGNORE_CASE),
            ""
        ).trim()
        return ProviderResponse(
            text = withoutMalformedTags.ifEmpty { "Sono qui con te." },
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
                val end = value.indexOf("]]", TAG_PREFIX.length)
                if (end < 0) return
                // Complete control tags are consumed even when unknown, so malformed provider
                // controls cannot leak into the avatar bubble or speech output.
                candidate.delete(0, end + 2)
                continue
            }
            output.append(candidate[0])
            candidate.deleteCharAt(0)
        }
    }
}

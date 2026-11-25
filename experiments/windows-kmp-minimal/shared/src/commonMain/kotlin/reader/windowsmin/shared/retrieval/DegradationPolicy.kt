package reader.windowsmin.shared.retrieval

/**
 * Degradation policy (D-06 / D-15 client boundary): when online generation is unavailable,
 * reading and local retrieval must still work.
 */
enum class ResponseMode { ONLINE_GENERATIVE, LOCAL_SEARCH_ONLY, NO_ANSWER }

object DegradationPolicy {

    fun decide(onlineAvailable: Boolean, localIndexReady: Boolean): ResponseMode {
        if (onlineAvailable) return ResponseMode.ONLINE_GENERATIVE
        if (localIndexReady) return ResponseMode.LOCAL_SEARCH_ONLY
        return ResponseMode.NO_ANSWER
    }

    fun localSearchOnly(query: String, index: KeywordIndex): List<SearchHit> = index.search(query)
}

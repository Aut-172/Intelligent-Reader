package reader.androidmin.shared.retrieval

/**
 * 在线生成不可用时的降级策略（D-06 / D-15 客户端边界）：
 * 一期不要求端侧生成模型；断网或供应方故障时，仍保持本地阅读与本地检索。
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

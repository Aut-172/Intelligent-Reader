package reader.windowsmin.shared.retrieval

/** Searchable text block: the shared text-block contract. */
data class TextBlock(
    val blockId: String,
    val page: Int,
    val order: Int,
    val text: String,
)

data class SearchHit(
    val blockId: String,
    val page: Int,
    val order: Int,
    val score: Int,
    val snippet: String,
)

/** Explainable keyword baseline shared by both clients. */
class KeywordIndex {
    private val blocks = mutableListOf<TextBlock>()

    fun index(blocks: List<TextBlock>) {
        this.blocks.addAll(blocks)
    }

    fun search(query: String, topK: Int = 5): List<SearchHit> {
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return emptyList()
        val hits = mutableListOf<SearchHit>()
        for (block in blocks) {
            val lower = block.text.lowercase()
            var score = 0
            for (token in tokens) {
                score += countOccurrences(lower, token.lowercase())
            }
            if (score > 0) {
                hits.add(SearchHit(block.blockId, block.page, block.order, score, snippet(block.text, query)))
            }
        }
        return hits.sortedWith(compareByDescending<SearchHit> { it.score }.thenBy { it.order }).take(topK)
    }

    fun size(): Int = blocks.size

    private fun tokenize(q: String): List<String> =
        q.split(Regex("[\\s，。、,.!?；;：:（）()\\[\\]【】]+")).filter { it.isNotBlank() }

    private fun countOccurrences(text: String, token: String): Int {
        var count = 0
        var idx = text.indexOf(token)
        while (idx >= 0) {
            count++
            idx = text.indexOf(token, idx + token.length)
        }
        return count
    }

    private fun snippet(text: String, query: String, max: Int = 60): String {
        val flat = text.replace('\n', ' ').trim()
        if (flat.length <= max) return flat
        val firstToken = tokenize(query).firstOrNull()
        val hit = firstToken?.let { flat.indexOf(it, ignoreCase = true) } ?: 0
        val start = (if (hit >= 0) hit - 10 else 0).coerceAtLeast(0)
        val end = (start + max).coerceAtMost(flat.length)
        return flat.substring(start, end).trim() + if (end < flat.length) "…" else ""
    }
}

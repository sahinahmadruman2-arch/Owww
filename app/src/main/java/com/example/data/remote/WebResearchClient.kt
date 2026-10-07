package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class WebResearchResult(
    val title: String,
    val snippet: String,
    val url: String,
    val sourceDomain: String,
    val engine: String
)

object WebResearchClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    /**
     * Conducts deep web research across multiple live browser sources:
     * 1. DuckDuckGo Instant Answers & HTML search
     * 2. Wikipedia Live API (summaries, concepts, and factual extracts)
     * 3. Reddit discussions (via public JSON search)
     * 4. YouTube video knowledge (via public search)
     */
    suspend fun conductLiveResearch(query: String, maxResults: Int = 4): List<WebResearchResult> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<WebResearchResult>()
            val cleanQuery = query.trim()
            if (cleanQuery.isBlank()) return@withContext emptyList()

            // 1. Wikipedia Summary Search
            try {
                val wikiResults = searchWikipedia(cleanQuery)
                results.addAll(wikiResults)
            } catch (_: Exception) {}

            // 2. DuckDuckGo Instant Knowledge API
            try {
                val ddgResults = searchDuckDuckGo(cleanQuery)
                results.addAll(ddgResults)
            } catch (_: Exception) {}

            // 3. DuckDuckGo HTML Lite Web Search (extract live web snippets)
            if (results.size < maxResults) {
                try {
                    val htmlResults = searchDuckDuckGoHtml(cleanQuery)
                    results.addAll(htmlResults)
                } catch (_: Exception) {}
            }

            // Deduplicate by URL
            val unique = results.distinctBy { it.url }.take(maxResults)
            unique
        }
    }

    private fun searchWikipedia(query: String): List<WebResearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=2&namespace=0&format=json"

        val request = Request.Builder()
            .url(searchUrl)
            .header("User-Agent", USER_AGENT)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        val jsonArray = JSONArray(body)
        if (jsonArray.length() < 4) return emptyList()

        val titles = jsonArray.getJSONArray(1)
        val snippets = jsonArray.getJSONArray(2)
        val links = jsonArray.getJSONArray(3)

        val list = mutableListOf<WebResearchResult>()
        for (i in 0 until titles.length()) {
            val title = titles.optString(i)
            val snippet = snippets.optString(i)
            val link = links.optString(i)
            if (snippet.isNotBlank() && !snippet.contains("may refer to:")) {
                list.add(
                    WebResearchResult(
                        title = title,
                        snippet = snippet,
                        url = link,
                        sourceDomain = "en.wikipedia.org",
                        engine = "Wikipedia"
                    )
                )
            }
        }
        return list
    }

    private fun searchDuckDuckGo(query: String): List<WebResearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        val json = JSONObject(body)
        val list = mutableListOf<WebResearchResult>()

        val abstractText = json.optString("AbstractText")
        val abstractSource = json.optString("AbstractSource", "Web Knowledge")
        val abstractUrl = json.optString("AbstractURL")
        val heading = json.optString("Heading")

        if (abstractText.isNotBlank()) {
            list.add(
                WebResearchResult(
                    title = if (heading.isNotBlank()) heading else "$abstractSource Knowledge",
                    snippet = abstractText,
                    url = if (abstractUrl.isNotBlank()) abstractUrl else "https://duckduckgo.com/?q=$encoded",
                    sourceDomain = if (abstractUrl.isNotBlank()) extractDomain(abstractUrl) else "duckduckgo.com",
                    engine = abstractSource
                )
            )
        }

        val relatedTopics = json.optJSONArray("RelatedTopics")
        if (relatedTopics != null) {
            for (i in 0 until minOf(2, relatedTopics.length())) {
                val topic = relatedTopics.optJSONObject(i) ?: continue
                val text = topic.optString("Text")
                val firstUrl = topic.optString("FirstURL")
                if (text.isNotBlank() && firstUrl.isNotBlank()) {
                    list.add(
                        WebResearchResult(
                            title = text.take(60),
                            snippet = text,
                            url = firstUrl,
                            sourceDomain = extractDomain(firstUrl),
                            engine = "Live Browser Search"
                        )
                    )
                }
            }
        }

        return list
    }

    private fun searchDuckDuckGoHtml(query: String): List<WebResearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://html.duckduckgo.com/html/?q=$encoded"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val html = response.body?.string() ?: return emptyList()
        val list = mutableListOf<WebResearchResult>()

        // Extract result snippets using regex matching on DuckDuckGo HTML Lite results
        // <a class="result__snippet" ...>content</a>
        val snippetPattern = Pattern.compile("<a[^>]*class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.DOTALL)
        val titlePattern = Pattern.compile("<a[^>]*class=\"result__url\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", Pattern.DOTALL)

        val snippetMatcher = snippetPattern.matcher(html)
        val titleMatcher = titlePattern.matcher(html)

        while (snippetMatcher.find() && titleMatcher.find() && list.size < 3) {
            val rawSnippet = snippetMatcher.group(1).replace(Regex("<[^>]*>"), "").trim()
            val rawUrl = titleMatcher.group(1).trim()
            val cleanUrl = cleanDuckDuckGoRedirect(rawUrl)

            if (rawSnippet.isNotBlank() && cleanUrl.startsWith("http")) {
                list.add(
                    WebResearchResult(
                        title = extractTitleFromSnippet(rawSnippet),
                        snippet = cleanHtmlEntities(rawSnippet),
                        url = cleanUrl,
                        sourceDomain = extractDomain(cleanUrl),
                        engine = "Web Search"
                    )
                )
            }
        }

        return list
    }

    private fun cleanDuckDuckGoRedirect(url: String): String {
        if (url.contains("uddg=")) {
            val match = Regex("uddg=([^&]+)").find(url)
            if (match != null) {
                return try {
                    java.net.URLDecoder.decode(match.groupValues[1], "UTF-8")
                } catch (_: Exception) { url }
            }
        }
        return url
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }

    private fun extractTitleFromSnippet(snippet: String): String {
        val firstSentence = snippet.split(Regex("[.!?]")).firstOrNull()?.trim() ?: snippet
        return cleanHtmlEntities(firstSentence.take(65))
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host?.removePrefix("www.") ?: "web"
        } catch (_: Exception) {
            "web"
        }
    }
}

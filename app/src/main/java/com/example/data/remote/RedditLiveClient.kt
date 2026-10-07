package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class RedditPostItem(
    val id: String,
    val title: String,
    val selftext: String,
    val subreddit: String,
    val author: String,
    val score: Int,
    val numComments: Int,
    val permalink: String,
    val fullUrl: String,
    val thumbnailUrl: String?,
    val createdUtc: Long
)

object RedditLiveClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val USER_AGENT = "UrBots7-NeuralAI/2.0 (Android; Knowledge Agent; +https://urbots7.ai)"

    suspend fun searchDiscussions(query: String, subreddit: String? = null, limit: Int = 10): List<RedditPostItem> {
        return withContext(Dispatchers.IO) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = if (subreddit != null && subreddit.isNotBlank() && subreddit != "all") {
                "https://www.reddit.com/r/$subreddit/search.json?q=$encodedQuery&restrict_sr=1&sort=relevance&limit=$limit"
            } else {
                "https://www.reddit.com/search.json?q=$encodedQuery&sort=relevance&limit=$limit"
            }

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }

                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                parseRedditJson(bodyStr)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun fetchHotPosts(subreddit: String = "technology", limit: Int = 10): List<RedditPostItem> {
        return withContext(Dispatchers.IO) {
            val url = "https://www.reddit.com/r/$subreddit/hot.json?limit=$limit"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                parseRedditJson(bodyStr)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun parseRedditJson(jsonString: String): List<RedditPostItem> {
        val list = mutableListOf<RedditPostItem>()
        try {
            val root = JSONObject(jsonString)
            val dataObj = root.optJSONObject("data") ?: return emptyList()
            val childrenArr = dataObj.optJSONArray("children") ?: return emptyList()

            for (i in 0 until childrenArr.length()) {
                val child = childrenArr.optJSONObject(i) ?: continue
                val p = child.optJSONObject("data") ?: continue

                val id = p.optString("id", "")
                val title = p.optString("title", "").trim()
                if (title.isEmpty()) continue

                val selftext = p.optString("selftext", "").trim()
                val subreddit = p.optString("subreddit", "reddit")
                val author = p.optString("author", "anonymous")
                val score = p.optInt("score", 0)
                val numComments = p.optInt("num_comments", 0)
                val permalink = p.optString("permalink", "")
                val fullUrl = if (permalink.isNotEmpty()) "https://www.reddit.com$permalink" else p.optString("url", "")
                val createdUtc = p.optLong("created_utc", System.currentTimeMillis() / 1000) * 1000

                var thumb: String? = p.optString("thumbnail", "")
                if (thumb.isNullOrBlank() || thumb == "default" || thumb == "self" || thumb == "nsfw") {
                    thumb = null
                }

                list.add(
                    RedditPostItem(
                        id = id,
                        title = title,
                        selftext = selftext,
                        subreddit = subreddit,
                        author = author,
                        score = score,
                        numComments = numComments,
                        permalink = permalink,
                        fullUrl = fullUrl,
                        thumbnailUrl = thumb,
                        createdUtc = createdUtc
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }
}

package eu.kanade.tachiyomi.extension.all.boommanga

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

/**
 * Springboard that accepts https://m.boommanga.com/detail/xxxxxx intents and redirects them to
 * the main Tachiyomi process.
 */
class BoomMangaUrlActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pathSegments = intent?.data?.pathSegments
        if (pathSegments != null && pathSegments.size > 1) {
            val id = if (pathSegments[0].equals("cn") && pathSegments.size > 2) {
                pathSegments[2]
            } else {
                pathSegments[1]
            }

            val mainIntent = Intent().apply {
                action = "eu.kanade.tachiyomi.SEARCH"
                putExtra("query", "${BoomManga.PREFIX_ID_SEARCH}$id")
                putExtra("filter", packageName)
            }

            try {
                startActivity(mainIntent)
            } catch (e: ActivityNotFoundException) {
                Log.e("BoomMangaUrl", e.toString())
            }

        } else {
            Log.e("BoomMangaUrl", "could not parse uri from intent $intent")
        }

        finish()
        exitProcess(0)
    }
}

package eu.kanade.tachiyomi.extension.all.comicake

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

/**
 * Springboard that accepts https://url/base/series/xxxxxx intents and redirects them to
 * the main Tachiyomi process.
 */
class ComiCakeUrlActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pathSegments = intent?.data?.pathSegments
        if (pathSegments != null && pathSegments.size > 1) {
            // Loop path segments to find series slug
            pathSegments.forEachIndexed { index, segment ->
                if (segment == "series" && (index + 1) < pathSegments.size) {
                    val id = pathSegments[index + 1]
                    val mainIntent = Intent().apply {
                        action = "eu.kanade.tachiyomi.SEARCH"
                        putExtra("query", "${ComiCake.PREFIX_ID_SEARCH}$id")
                        putExtra("filter", packageName)
                    }

                    try {
                        startActivity(mainIntent)
                    } catch (e: ActivityNotFoundException) {
                        Log.e("ComiCakeUrl", e.toString())
                    }
                }
            }

        } else {
            Log.e("ComiCakeUrl", "could not parse uri from intent $intent")
        }

        finish()
        exitProcess(0)
    }
}

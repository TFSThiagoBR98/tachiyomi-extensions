package eu.kanade.tachiyomi.extension.all.toomics

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Headers
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.net.URLDecoder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

abstract class ToomicsGlobal(private val siteLang: String,
                             private val dateFormat: SimpleDateFormat,
                             override val lang: String = siteLang,
                             displayName: String = "") : ParsedHttpSource() {

    override val name = "Toomics (Only free chapters)" + (if (displayName.isNotEmpty()) " ($displayName)" else "")

    override val baseUrl = "https://global.toomics.com"

    override val supportsLatest = true

    override val client: OkHttpClient = super.client.newBuilder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", "$baseUrl/$siteLang")
        .add("User-Agent", USER_AGENT)

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/$siteLang/webtoon/free_all", headers)
    }

    // ToomicsGlobal does not have a popular list, so use recommended instead.
    override fun popularMangaSelector(): String = "li > div.visual"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        element.select("a").let {
            title = it.text()
            setUrlWithoutDomain(it.attr("href"))
        }
        thumbnail_url = element.select("img").attr("abs:data-original")
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/$siteLang/webtoon/free", headers)
    }

    override fun latestUpdatesSelector(): String = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val newHeaders = headersBuilder()
            .add("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val rbody = RequestBody.create(null, "toonData=$query&offset=0&limit=20")

        return POST("$baseUrl/$siteLang/webtoon/ajax_search", newHeaders, rbody)
    }

    override fun searchMangaSelector(): String = "div.recently_list ul li"

    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("a div.search_box dl dt span.title").text()
        thumbnail_url = element.select("div.search_box p.img img").attr("abs:src")

        // When the family mode is off, the url is encoded and is available in the onclick.
        element.select("a:not([href^=javascript])").let {
            if (it != null) {
                setUrlWithoutDomain(it.attr("href"))
            } else {
                val toonId = element.select("a").attr("onclick")
                    .substringAfter("Base.setDisplay('A', '")
                    .substringBefore("'")
                    .let { URLDecoder.decode(it, "UTF-8") }
                    .substringAfter("?toon=")
                    .substringBefore("&")
                url = "/$siteLang/webtoon/episode/toon/$toonId"
            }
        }
    }

    override fun searchMangaNextPageSelector(): String? = null

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val header = document.select("#glo_contents header.ep-cover_ch div.title_content")

        title = header.select("h1").text()
        author = header.select("p.type_box span.writer").text()
        artist = header.select("p.type_box span.writer").text()
        genre = header.select("p.type_box span.type").text().replace("/", ",")
        description = header.select("h2").text()
        thumbnail_url = document.select("head meta[property='og:image']").attr("content")
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return super.fetchChapterList(manga)
            .map { it.reversed() }
    }

    override fun chapterListSelector(): String = "li.normal_ep:has(.coin-type1)"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        val num = element.select("div.cell-num").text()
        val numText = if (num.isNotEmpty()) "$num - " else ""

        name = numText + element.select("div.cell-title")?.first()?.ownText()
        chapter_number = num.toFloatOrNull() ?: 0f
        date_upload = parseChapterDate(element.select("div.cell-time time").text()!!)
        scanlator = "Toomics"
        url = element.select("a").attr("onclick")
            .substringAfter("href='")
            .substringBefore("'")
    }

    override fun pageListParse(document: Document): List<Page> {
        val url = document.select("head meta[property='og:url']").attr("content")

        return document.select("div[id^=load_image_] img")
            .mapIndexed { i, el -> Page(i, url, el.attr("abs:data-original"))}
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    override fun imageRequest(page: Page): Request {
        val newHeaders = headers.newBuilder()
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun parseChapterDate(date: String) : Long {
        return try {
            dateFormat.parse(date).time
        } catch (e: ParseException) {
            0L
        }
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.92 Safari/537.36"
    }
}

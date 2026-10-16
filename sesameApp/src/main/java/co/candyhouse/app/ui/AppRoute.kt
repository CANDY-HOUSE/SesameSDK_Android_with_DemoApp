package co.candyhouse.app.ui

import org.json.JSONObject

/** Current Biz route from home.route. Biz decides which routes are tabs and which draw their own header. */
data class AppRoute(val path: String, val tab: Boolean, val pageHeader: Boolean) {
    val home get() = path == "/"

    companion object {
        val HOME = AppRoute("/", tab = true, pageHeader = false)

        fun from(json: JSONObject) = AppRoute(json.optString("path"), json.optBoolean("tab"), json.optBoolean("pageHeader"))
    }
}

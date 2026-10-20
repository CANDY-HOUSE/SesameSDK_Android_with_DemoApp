package co.candyhouse.app.ui

import org.json.JSONObject

/** Current Biz route from home.route. Biz decides which routes are tabs and whether a page layer consumes back. */
data class AppRoute(val path: String, val tab: Boolean, val modal: Boolean) {
    /** System back finishes the Activity only on a tab page without an open Biz layer. */
    val exits get() = tab && !modal

    companion object {
        val HOME = AppRoute("/", tab = true, modal = false)

        fun from(json: JSONObject) = AppRoute(json.optString("path"), json.optBoolean("tab"), json.optBoolean("modal"))
    }
}

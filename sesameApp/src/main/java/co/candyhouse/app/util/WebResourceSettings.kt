package co.candyhouse.app.util

import android.content.Context

/** Shared by foreground and background WebViews; offline resources are the default. */
object WebResourceSettings {
    private fun prefs(context: Context) = context.getSharedPreferences("web_resource_prefs", Context.MODE_PRIVATE)
    fun isBundled(context: Context) = prefs(context).getBoolean("bundled", true)
    fun setBundled(context: Context, bundled: Boolean) {
        prefs(context).edit().putBoolean("bundled", bundled).apply()
    }
}

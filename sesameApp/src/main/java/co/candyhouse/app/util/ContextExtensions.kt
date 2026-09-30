package co.candyhouse.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.dp(value: Int) = (value * resources.displayMetrics.density).toInt()

fun Context.openExternalUrl(uri: Uri) {
    require(uri.scheme in setOf("https", "http", "mailto", "tel"))
    startActivity(Intent(Intent.ACTION_VIEW, uri))
}

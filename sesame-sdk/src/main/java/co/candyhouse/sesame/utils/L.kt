package co.candyhouse.sesame.utils

import android.os.Looper
import android.util.Log
import co.candyhouse.sesame.BuildConfig

class L {
    companion object {

        @JvmStatic
        fun i(tag: String = "hcia", msg: String) {
            if (BuildConfig.DEBUG) {
                val stackTrace = Thread.currentThread().stackTrace// StackTraceElement[]
                val index = 3
                val className = stackTrace[index].fileName
                val lineNumber = stackTrace[index].lineNumber
                val stringBuilder = StringBuilder()
                stringBuilder.append(isUIthread()).append("(").append(className).append(":")
                    .append(lineNumber).append(") ")
                Log.i(tag, "$msg  | $stringBuilder")
            }
        }

        @JvmStatic
        fun d(tag: String = "hcia", msg: String) {
            if (BuildConfig.DEBUG) {
                val stackTrace = Thread.currentThread().stackTrace// StackTraceElement[]
                val index = 3
                val className = stackTrace[index].fileName
                val lineNumber = stackTrace[index].lineNumber
                val stringBuilder = StringBuilder()
                stringBuilder.append(isUIthread()).append("(").append(className).append(":")
                    .append(lineNumber).append(") ")
                Log.d(tag, "$msg  | $stringBuilder")
            }
        }

        @JvmStatic
        fun w(tag: String = "hcia", msg: String) {
            if (BuildConfig.DEBUG) {
                val stackTrace = Thread.currentThread().stackTrace// StackTraceElement[]
                val index = 3
                val className = stackTrace[index].fileName
                val lineNumber = stackTrace[index].lineNumber
                val stringBuilder = StringBuilder()
                stringBuilder.append(isUIthread()).append("(").append(className).append(":")
                    .append(lineNumber).append(") ")
                Log.w(tag, "$msg  | $stringBuilder")
            }
        }

        @JvmStatic
        fun e(tag: String = "hcia", msg: String) {
            if (BuildConfig.DEBUG) {
                val stackTrace = Thread.currentThread().stackTrace// StackTraceElement[]
                val index = 3
                val className = stackTrace[index].fileName
                val lineNumber = stackTrace[index].lineNumber
                val stringBuilder = StringBuilder()
                stringBuilder.append(isUIthread()).append("(").append(className).append(":")
                    .append(lineNumber).append(") ")
                Log.e(tag, "$msg  | $stringBuilder")
            }
        }

        @JvmStatic
        fun e(tag: String = "hcia", msg: String, tr: Throwable) {
            if (BuildConfig.DEBUG) {
                val stackTrace = Thread.currentThread().stackTrace// StackTraceElement[]
                val index = 3
                val className = stackTrace[index].fileName
                val lineNumber = stackTrace[index].lineNumber
                val stringBuilder = StringBuilder()
                stringBuilder.append(isUIthread()).append("(").append(className).append(":")
                    .append(lineNumber).append(") ")
                Log.e(tag, "$msg  | $stringBuilder", tr)
            }
        }

        private fun isUIthread(): String {
            val isM = Looper.getMainLooper() == Looper.myLooper()
            return if (isM) "主線成" else "副線成"
        }

    }
}
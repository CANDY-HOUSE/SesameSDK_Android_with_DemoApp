package co.utils

import android.os.Looper
import android.util.Log

internal class L {
    companion object {
        fun d(tag: String = "hcia", msg: String) {
            val stackTrace = Thread.currentThread().getStackTrace()// StackTraceElement[]
            val index = 3
            val className = stackTrace[index].getFileName()
            var methodName = stackTrace[index].getMethodName()
            val lineNumber = stackTrace[index].getLineNumber()
//
            methodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
//
            val stringBuilder = StringBuilder()
            stringBuilder.append("(").append(className).append(":").append(lineNumber).append(") ")
            Log.d(tag, msg + "  |" + methodName + " " + isUIthread() + stringBuilder)
        }

        private fun isUIthread(): String {
            val isM = Looper.getMainLooper() == Looper.myLooper()
            return if (isM) "主線成" else "副線成"
        }

    }
}
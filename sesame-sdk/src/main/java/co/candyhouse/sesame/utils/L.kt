package co.candyhouse.sesame.utils

import android.os.Looper
import android.util.Log
import co.candyhouse.sesame2.BuildConfig.BUILD_TYPE

 class L {
    companion object {
        val tag="jplog"
        fun d(tag: String = "hcia", msg: String) {
            if (BUILD_TYPE == "debug") {
                val stackTrace = Thread.currentThread().getStackTrace()// StackTraceElement[]
//                val thName = Thread.currentThread().name
                val index = 3
                val className = stackTrace[index].getFileName()
                val lineNumber = stackTrace[index].getLineNumber()
                val stringBuilder = StringBuilder()
                stringBuilder.append("(").append(className).append(":").append(lineNumber).append(") ")
                Log.d(tag, msg + "  "  + "" + stringBuilder+""+isUIthread())
            }
        }
        fun l(vararg msg:String? ) {
            if (BUILD_TYPE == "debug") {
                val stackTrace = Thread.currentThread().getStackTrace()// StackTraceElement[]
//                val thName = Thread.currentThread().name
                val index = 3
                val className = stackTrace[index].className
                val methodName = stackTrace[index].methodName
                val stringBuilder = StringBuilder()
                stringBuilder.append("[${isUIthread()}],")
                stringBuilder.append("[${className}],")
                stringBuilder.append("[${methodName}],")

                msg.forEach {
                    it?.apply {
                        stringBuilder.append("[${it}]")
                    }

                }

                Log.d(tag, stringBuilder.toString())
            }
        }
        private fun isUIthread(): String {
            val isM = Looper.getMainLooper() == Looper.myLooper()
            return if (isM) "主線成" else "副線成"
        }
    }
}
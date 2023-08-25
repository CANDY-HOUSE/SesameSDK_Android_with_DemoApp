package co.utils

import android.os.Looper
import android.util.Log
import co.candyhouse.app.BuildConfig
import co.candyhouse.sesame.utils.L

internal class L {
    companion object {
        val tag="jplog"
        fun d(tag: String = "hcia", msg: String) {
//            if (BuildConfig.DEBUG) {
            val stackTrace = Thread.currentThread().stackTrace// StackTraceElement[]
            val index = 3
            val className = stackTrace[index].fileName
//            var methodName = stackTrace[index].getMethodName()
            val lineNumber = stackTrace[index].lineNumber
//
//            methodName = methodName.substring(0, 1).uppercase() + methodName.substring(1);

            val stringBuilder = StringBuilder()
            stringBuilder.append(isUIthread()).append("(").append(className).append(":").append(lineNumber).append(") ")
//            Log.d(tag, msg + "  |" + methodName + " " + isUIthread() + stringBuilder)
            Log.d(tag, "$msg  | $stringBuilder")
//            }
        }
        fun l(vararg msg:String? ) {
            if (co.candyhouse.sesame2.BuildConfig.BUILD_TYPE == "debug") {
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

                Log.d(L.tag, stringBuilder.toString())
            }
        }
        private fun isUIthread(): String {
            val isM = Looper.getMainLooper() == Looper.myLooper()
            return if (isM) "主線成" else "副線成"
        }

    }
}
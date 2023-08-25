package co.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class CrashUtils : Thread.UncaughtExceptionHandler {
    private var crashListener: CrashListener? = null
    private var isJumpError = true
    private var context: Context? = null
    private var errorInfo: ErrorBean? = null
    private var isDebug = false
    fun init(context: Context?): CrashUtils {
        this.context = context
        errorInfo = ErrorBean()
        Thread.setDefaultUncaughtExceptionHandler(this)
        return this
    }

    fun setDebug(debug: Boolean): CrashUtils {
        isDebug = debug
        return this
    }

    fun setCrashListener(crashListener: CrashListener?) {
        this.crashListener = crashListener
    }

    fun setJumpError(isJumpError: Boolean): CrashUtils {
        this.isJumpError = isJumpError
        return this
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (crashListener != null) {
            crashListener!!.CrashInfo(ex)
        } else {
            Log.e("CrashHandler", "No crash listening set")
        }
        errorInfo = Covers(ex)
        writeToFile(context, errorInfo!!.throwString(ex))
    }

    fun writeToFile(context: Context?, msg: String?) {
        try {
            val fatherFile = File(context!!.filesDir, "crash")
            if (!fatherFile.exists()) {
                fatherFile.mkdirs()
            }
            val name = "crash" + SimpleDateFormat("yyyyMMddHHmmss").format(Date())
            val file = File(fatherFile, name)
            val printWriter = PrintWriter(FileOutputStream(file.absoluteFile))
            printWriter.write(msg)
            printWriter.flush()
            printWriter.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun Covers(t: Throwable): ErrorBean? {
        errorInfo!!.date = Date().toString()
        errorInfo!!.cause = t.cause
        errorInfo!!.localizedMessage = t.localizedMessage
        errorInfo!!.message = t.message
        val resultList: MutableList<ErrorBean.StackTraceBean> = ArrayList(t.stackTrace.size)
        for (s in t.stackTrace) {
            val st = ErrorBean.StackTraceBean()
            st.className = s.className
            st.fileName = s.fileName
            st.lineNumber = s.lineNumber
            st.methodName = s.methodName
            st.isNativeMethod = s.isNativeMethod
            resultList.add(st)
        }
        errorInfo!!.stackTrace = resultList
        errorInfo!!.type = t.cause
        try {
            val pm = context!!.packageManager
            val pi = pm.getPackageInfo(context!!.packageName, 0)
            errorInfo!!.brand = Build.BRAND
            errorInfo!!.model = Build.MODEL
            errorInfo!!.sdk_version_name = Build.VERSION.RELEASE
            errorInfo!!.sdk_version_code = Build.VERSION.SDK_INT.toString() + ""
            errorInfo!!.app_version_name = pi.versionName + ""
            errorInfo!!.app_version_code = pi.versionCode.toString() + ""
            errorInfo!!.android_id = getAndroidID(context)
            errorInfo!!.package_name = context!!.packageName
            errorInfo!!.app_name = pi.applicationInfo.loadLabel(pm).toString()
            errorInfo!!.isDebug = isDebug.toString() + ""
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return errorInfo
    }

    @SuppressLint("ApplySharedPref")
    fun saveError(str: String?) {
        val sharedPreferences = context!!.getSharedPreferences("data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("error", str)
        editor.commit()
    }

    fun readError(): String? {
        val sharedPreferences = context!!.getSharedPreferences("data", Context.MODE_PRIVATE)
        return sharedPreferences.getString("error", "None")
    }

    interface CrashListener {
        fun CrashInfo(ex: Throwable?)
    }

    class ErrorBean {
        var brand = Build.BRAND
        var model = Build.MODEL
        var app_name: String? = null
        var package_name: String? = null
        var app_version_name: String? = null
        var app_version_code: String? = null
        var sdk_version_name: String? = null
        var sdk_version_code: String? = null
        var android_id: String? = null
        var mobile: String? = null
        var isDebug: String? = null
        var type: Throwable? = null
        var cause: Any? = null
        var localizedMessage: String? = null
        var message: String? = null
        var stackTrace: List<StackTraceBean>? = null
        var date: String? = null
        fun isNullString(o: Any?): String {
            return o?.toString() ?: "nullObject "
        }

        fun toNewString(): String {
            val jsonObject = JSONObject()
            try {
                jsonObject.put("app_name", isNullString(app_name))
                jsonObject.put("app_version_name", isNullString(app_version_name))
                jsonObject.put("sdk_version_name", isNullString(sdk_version_name))
                jsonObject.put("package_name", isNullString(package_name))
                jsonObject.put("brand", isNullString(brand))
                jsonObject.put("model", isNullString(model))
                jsonObject.put("app_version_code", isNullString(app_version_code))
                jsonObject.put("sdk_version_code", isNullString(sdk_version_code))
                jsonObject.put("android_id", isNullString(android_id))
                jsonObject.put("type", isNullString(type))
                jsonObject.put("message", isNullString(message))
                jsonObject.put("data", SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
                jsonObject.put("cause", isNullString(cause))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return jsonObject.toString()
        }

        fun throwString(t: Throwable): String {
            val stringBuilder = StringBuilder()
            stringBuilder.append(toNewString())
            stringBuilder.append("\n")
            stringBuilder.append("------------------------throwable-------------------\n")
            for (s in t.stackTrace) {
                val st = StackTraceBean()
                st.className = s.className
                st.fileName = s.fileName
                st.lineNumber = s.lineNumber
                st.methodName = s.methodName
                st.isNativeMethod = s.isNativeMethod
                stringBuilder.append(
                    """
    $s
    
    """.trimIndent()
                )
            }
            return stringBuilder.toString()
        }

        override fun toString(): String {
            return "{" +
                    "\"app_name\":" + isNull(app_name) + "," +
                    "\"app_version_name\":" + isNull(app_version_name) + "," +
                    "\"sdk_version_name\":" + isNull(sdk_version_name) + "," +
                    "\"errorFile\":" + isNull(stackTrace!![0].fileName) + "," +
                    "\"errorLineNum\":" + isNull(stackTrace!![0].lineNumber) + "," +
                    "\"errorMethod\":" + isNull(stackTrace!![0].methodName) + "," +
                    "\"package_name\":" + isNull(package_name) + "," +
                    "\"brand\":" + isNull(brand) + "," +
                    "\"model\":" + isNull(model) + "," +
                    "\"app_version_code\":" + isNull(app_version_code) + "," +
                    "\"sdk_version_code\":" + isNull(sdk_version_code) + "," +
                    "\"android_id\":" + isNull(android_id) + "," +
                    "\"mobile\":" + isNull(mobile) + "," +
                    "\"isDebug\":" + isNull(isDebug) + "," +
                    "\"type\":" + isNull(type) + "," +
                    "\"date\":" + System.currentTimeMillis() + "," +
                    "\"cause\":" + isNull(cause) + "," +
                    "\"localizedMessage\":" + localizedMessage + "," +
                    "\"message\":" + message + "," +
                    "\"stackTrace\":" + stackTrace.toString() +
                    "}"
        }

        fun isNull(o: Any?): String {
            return if (o == null) "\"\" " else "\"" + o + "\""
        }

        class StackTraceBean : Serializable {
            /**
             * className : com.pandarupees.app.frame.ui.activity.Test
             * fileName : Test.java
             * lineNumber : 12
             * methodName : test
             * nativeMethod : false
             */
            var className: String? = null
            var fileName: String? = null
            var lineNumber = 0
            var methodName: String? = null
            var isNativeMethod = false
            override fun toString(): String {
                return "{" +
                        "\"className\":" + isNull(className) + "," +
                        "\"fileName\":" + isNull(fileName) + "," +
                        "\"lineNumber\":" + isNull(lineNumber) + "," +
                        "\"methodName\":" + isNull(methodName) + "," +
                        "\"nativeMethod\":" + isNull(isNativeMethod) +
                        "}"
            }

            fun isNull(o: Any?): String {
                return if (o == null) "\"\" " else "\"" + o + "\""
            }
        }
    }

    companion object {
        private var INSTANCE: CrashUtils? = null
        val instance: CrashUtils
            get() {
                if (INSTANCE == null) {
                    INSTANCE = CrashUtils()
                }
                return INSTANCE!!
            }

        @SuppressLint("HardwareIds")
        fun getAndroidID(context: Context?): String {
            val id = Settings.Secure.getString(
                context!!.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            return if ("9774d56d682e549c" == id) "" else id ?: ""
        }
    }
}
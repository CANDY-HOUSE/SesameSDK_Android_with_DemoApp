package co.candyhouse.sesame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import co.candyhouse.sesame.utils.L
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

abstract class BaseFG<T : ViewBinding> : Fragment() {

    lateinit var bind: T

    private val cameraPermissionRequestCode = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            if (!::bind.isInitialized) {
                bind = getViewBinder()
            }
            bind.root
        } catch (e: Exception) {
            e.printStackTrace()

            L.d("BaseFG", "Error initializing view binding")
            null
        }
    }

    abstract fun getViewBinder(): T

    private fun initBind(inflater: LayoutInflater, container: ViewGroup?) {
        val type = this.javaClass.genericSuperclass
        if (type is ParameterizedType) {
            val tClass = type.actualTypeArguments[0] as Class<T>
            try {
                val method = tClass.getMethod(
                    "inflate",
                    LayoutInflater::class.java,
                    ViewGroup::class.java,
                    Boolean::class.java
                )
                bind = method.invoke(null, inflater, container, false) as T
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
                try {
                    val method = tClass.getMethod("inflate", LayoutInflater::class.java)
                    bind = method.invoke(null, inflater) as T
                } catch (e: NoSuchMethodException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }

    fun setStatusColor(color: Int) {
        // 获取宿主Activity的Window对象
        val window = activity?.window
        window?.apply {
            // 清除透明状态栏标志
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            // 添加标志以绘制系统栏背景
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            // 设置状态栏颜色
            statusBarColor = ContextCompat.getColor(context, color)
        }
    }

    override fun onResume() {
        super.onResume()
        L.d("BaseFG", "baseFragment===" + this::class.java.simpleName)
    }

}
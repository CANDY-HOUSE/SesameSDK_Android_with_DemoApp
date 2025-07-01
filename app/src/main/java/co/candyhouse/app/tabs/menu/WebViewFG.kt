package co.candyhouse.app.tabs.menu

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgWebviewBinding
import co.candyhouse.sesame.utils.L
import co.utils.AnalyticsUtil

class WebViewFG : Fragment() {
    private val tag = "WebViewFG"

    private var _binding: FgWebviewBinding? = null
    private val binding get() = _binding!!

    private val url by lazy { arguments?.getString("url") ?: "" }
    private val where by lazy { arguments?.getString("where") ?: "" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FgWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupWebView()

        L.d(tag, "url=$url where=$where")
        if (url.isNotEmpty()) {
            binding.webView.loadUrl(url)
        }

        // 埋点：用户打开了网页
        AnalyticsUtil.logScreenView(tag + "_" + where)
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            if (!findNavController().popBackStack()) {
                // 如果返回栈为空，返回到主页
                findNavController().navigate(R.id.deviceListPG)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = 0
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    binding.progressBar.apply {
                        progress = newProgress
                        visibility = if (newProgress < 99) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
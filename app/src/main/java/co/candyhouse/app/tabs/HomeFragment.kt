package co.candyhouse.app.tabs

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.base.setPage
import co.candyhouse.app.base.view.IBaseView
import co.candyhouse.app.ext.aws.AWSConfig
import co.candyhouse.app.tabs.friend.ContactsWebViewManager
import co.candyhouse.app.tabs.menu.BarMenuItem
import co.candyhouse.app.tabs.menu.CustomAdapter
import co.candyhouse.app.tabs.menu.ItemUtils
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.sesame.utils.L
import co.utils.alerts.ext.inputNameAlert
import co.utils.alertview.fragments.toastMSG
import co.utils.safeNavigate
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.regions.Region
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient
import com.amazonaws.services.cognitoidentityprovider.model.ListUsersRequest
import com.amazonaws.services.cognitoidentityprovider.model.ListUsersResult
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.OnBalloonClickListener
import com.skydoves.balloon.OnBalloonOutsideTouchListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

abstract class HomeFragment<T : ViewBinding> : BaseNFG<T>(), IBaseView {

    lateinit var customListBalloon: Balloon
    private val customAdapter by lazy {
        CustomAdapter(object : CustomAdapter.CustomViewHolder.Delegate {
            override fun onCustomItemClick(customItem: BarMenuItem) {
                customListBalloon.dismiss()
                when (customItem.index) {
                    1 -> {
                        safeNavigate(R.id.to_regist)
                    }

                    2 -> {
                        safeNavigate(R.id.to_scan)
                    }

                    3 -> {
                        showAddContactDialog()
                    }
                }
            }
        })
    }

    private fun String.isValidEmail(): Boolean {
        return this.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }

    fun showAddContactDialog() {
        context?.inputNameAlert(getString(R.string.add_contacts), null) {
            topHinText = getString(R.string.pleaseemail)
            hintText = "friend@email.com"
            hintColor = ContextCompat.getColor(context, R.color.text_hint) // 使用 ContextCompat 获取颜色
            confirmButtonWithText("OK") { alert, name ->
                if (name.isValidEmail()) {
                    getUserByEmail(name)
                } else {
                    Toast.makeText(requireContext(), "无效的邮箱地址", Toast.LENGTH_SHORT).show()
                }

                dismiss()
            }
            cancelButton(getString(R.string.cancel))
        }?.show()
    }

    private fun getUserByEmail(email: String) {
        if (!AWSMobileClient.getInstance().isSignedIn) {
            toastMSG(getString(R.string.loginNeed))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val awsConfiguration = AWSConfiguration(JSONObject(AWSConfig.jpDevTeam))

                val cognitoIdentityProvider =
                    AmazonCognitoIdentityProviderClient(AWSMobileClient.getInstance().credentials)
                cognitoIdentityProvider.setRegion(Region.getRegion("ap-northeast-1"))

                val userPool = CognitoUserPool(requireContext(), awsConfiguration)
                val request = ListUsersRequest().apply {
                    this.userPoolId = userPool.userPoolId
                    this.filter = "email = \"$email\""
                }
                val result: ListUsersResult = cognitoIdentityProvider.listUsers(request)
                val user = result.users?.firstOrNull()
                var subId: String? = null

                user?.attributes?.forEach {
                    if (it.name == "sub") {
                        subId = it.value

                        CHLoginAPIManager.addFriend(subId!!) {
                            it.onSuccess {
                                L.d("adduser", "addFriend success: ")
                                view?.post {
                                    ContactsWebViewManager.setPendingRefresh()
                                    ContactsWebViewManager.setPendingDetailNavigation(email, subId)
                                    findNavController().navigateUp()
                                    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)?.setPage(1)
                                }
                            }
                            it.onFailure {
                                L.d("adduser", "掃描頁面 friend 失敗 it: $it")
                            }
                        }
                    }
                }

                if (subId == null) {
                    L.d("adduser", "subId not found in user attributes")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                L.d("getUserByEmail", "error: ${e.message}")
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeCustomListBalloon(view)

        // 每次视图创建都需要执行
        setupUI()
        setupListeners()
        observeViewModelData(view)
    }

    private fun initializeCustomListBalloon(view: View) {
        val menuBtn = view.findViewById<View>(R.id.right_icon).apply {
            setOnClickListener {
                customListBalloon.showAlignBottom(it)
            }
        }
        customListBalloon =
            Balloon.Builder(menuBtn.context).setLayout(R.layout.layout_custom_list).setArrowSize(12)
                .setArrowOrientation(ArrowOrientation.TOP).setArrowPosition(0.85f).setTextSize(12f)
                .setCornerRadius(4f).setBalloonAnimation(BalloonAnimation.CIRCULAR)
                .setBackgroundColorResource(R.color.menu_bg)
                .setBalloonAnimation(BalloonAnimation.FADE).setDismissWhenClicked(true)
                .setOnBalloonClickListener(object : OnBalloonClickListener {
                    override fun onBalloonClick(view: View) {
                    }
                }).setDismissWhenClicked(true)
                .setOnBalloonOutsideTouchListener(object : OnBalloonOutsideTouchListener {
                    override fun onBalloonOutsideTouch(view: View, event: MotionEvent) {
                        menuBtn.isClickable = false
                        customListBalloon.dismiss()
                        menuBtn.postDelayed({
                            menuBtn.isClickable = true
                        }, 300)
                    }
                }).build()

        customListBalloon.getContentView().findViewById<RecyclerView>(R.id.list_recyclerView)
            .apply {
                setHasFixedSize(true)
                adapter = customAdapter
                customAdapter.addCustomItem(ItemUtils.getCustomSamples(requireContext()))
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            }
    }

}
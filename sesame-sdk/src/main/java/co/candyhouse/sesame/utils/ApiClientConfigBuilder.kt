package co.candyhouse.sesame.utils

import android.content.Context
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
import com.amazonaws.regions.Regions
import java.util.Locale

/**
 * 通用配置构建器
 *
 * @author frey on 2025/12/24
 */
object ApiClientConfigBuilder {

    private const val DEFAULT_REGION = "ap-northeast-1"
    private val USER_AGENT_PATTERN = """\b[a-z]{2}_[A-Z]{2}\b""".toRegex()

    /**
     * 通用的客户端配置
     */
    fun createClientConfiguration(): ClientConfiguration.() -> Unit = {
        userAgent = userAgent?.replace(
            regex = USER_AGENT_PATTERN,
            replacement = Locale.getDefault().toString()
        ) ?: userAgent
    }

    /**
     * 构建API客户端工厂
     */
    fun buildApiClientFactory(
        credentialsProvider: AWSCredentialsProvider,
        apiKey: String? = null,
        region: String = DEFAULT_REGION
    ): ApiClientFactory {
        return ApiClientFactory()
            .credentialsProvider(credentialsProvider)
            .apply { apiKey?.let { apiKey(it) } }
            .region(region)
            .clientConfiguration(
                ClientConfiguration().apply(createClientConfiguration())
            )
    }

    /**
     * 构建凭证提供者
     */
    fun createCredentialsProvider(
        appContext: Context,
        identityPoolId: String,
        region: Regions
    ): CognitoCachingCredentialsProvider {
        return CognitoCachingCredentialsProvider(
            appContext,
            identityPoolId,
            region
        )
    }
}
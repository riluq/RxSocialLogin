package com.github.windsekirun.rxsociallogin.wordpress

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebView
import com.github.kittinunf.fuel.httpPost
import com.github.windsekirun.rxsociallogin.OAuthConstants
import com.github.windsekirun.rxsociallogin.RxSocialLogin
import com.github.windsekirun.rxsociallogin.intenal.fuel.toResultObservable
import com.github.windsekirun.rxsociallogin.intenal.oauth.BaseOAuthActivity
import com.github.windsekirun.rxsociallogin.intenal.oauth.OAuthWebViewClient
import com.github.windsekirun.rxsociallogin.intenal.model.PlatformType
import com.github.windsekirun.rxsociallogin.intenal.webview.EnhanceWebView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_oauth.*

class WordpressOAuthActivity : BaseOAuthActivity() {
    private val config: WordpressConfig by lazy { RxSocialLogin.getPlatformConfig(PlatformType.WORDPRESS) as WordpressConfig }

    @SuppressLint("SetJavaScriptEnabled")
    override fun init() {
        val url = "${OAuthConstants.WORDPRESS_URL}?client_id=${config.clientId}&" +
                "redirect_uri=${config.redirectUri}&response_type=code"

        webView.setWebViewHandler(object : EnhanceWebView.EnhanceWebViewHandler {
            override fun onPageFinished(view: WebView?, url: String?) {}

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?, uri: Uri?, scheme: String?,
                                                  host: String?, parameters: MutableMap<String, String>?): Boolean {
                try {
                    if (url?.contains("?code=") == false) return false
                    requestOAuthToken(url!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return false
            }
        })

        webView.setEnableGoBack(true)
        webView.enableFormUpload(this)
        webView.setUrl(url)

        setToolbar(config.activityTitle)
    }

    override fun onBackPressed() {
        if (webView.onBackPressed()) {
            super.onBackPressed()
        }
    }

    private fun requestOAuthToken(code: String) {
        val parameters = listOf(
                "grant_type" to "authorization_code",
                "redirect_uri" to config.redirectUri,
                "client_id" to config.clientId,
                "client_secret" to config.clientSecret,
                "code" to code)

        val header = "Content-Type" to "application/json"

        disposable = OAuthConstants.WORDPRESS_OAUTH.httpPost(parameters)
                .header(header)
                .toResultObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { result, error ->
                    if (error == null && result.component1() != null) {
                        finishActivity(result.component1() as String)
                    } else {
                        finishActivity()
                    }
                }
    }
}
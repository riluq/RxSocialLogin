package com.github.windsekirun.rxsociallogin.disqus

import android.app.Activity
import android.content.Intent
import com.github.windsekirun.rxsociallogin.OAuthConstants
import com.github.windsekirun.rxsociallogin.SocialLogin
import com.github.windsekirun.rxsociallogin.intenal.net.OkHttpHelper
import com.github.windsekirun.rxsociallogin.intenal.oauth.BaseOAuthActivity
import com.github.windsekirun.rxsociallogin.model.LoginResultItem
import com.github.windsekirun.rxsociallogin.model.PlatformType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import pyxis.uzuki.live.richutilskt.utils.createJSONObject
import pyxis.uzuki.live.richutilskt.utils.getJSONString
import pyxis.uzuki.live.richutilskt.utils.isEmpty

class DisqusLogin(activity: Activity) : SocialLogin(activity) {
    private val compositeDisposable = CompositeDisposable()
    private val config: DisqusConfig by lazy { getConfig(PlatformType.DISQUS) as DisqusConfig }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == OAuthConstants.DISQUS_REQUEST_CODE) {
            val jsonStr = data!!.getStringExtra(BaseOAuthActivity.RESPONSE_JSON) ?: "{}"
            analyzeResult(jsonStr)
        } else if (requestCode == OAuthConstants.DISQUS_REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            responseFail(PlatformType.DISQUS)
        }
    }

    override fun onLogin() {
        val intent = Intent(activity, DisqusOAuthActivity::class.java)
        activity?.startActivityForResult(intent, OAuthConstants.DISQUS_REQUEST_CODE)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
    }

    private fun analyzeResult(jsonStr: String) {
        val result = jsonStr.createJSONObject()
        if (result == null) {
            responseFail(PlatformType.DISQUS)
            return
        }

        val accessToken = result.getJSONString("access_token", "")
        if (accessToken.isEmpty()) {
            responseFail(PlatformType.DISQUS)
            return
        }

        val requestUrl = "https://disqus.com/api/3.0/users/details.json" +
                "?access_token=$accessToken&api_key=${config.clientId}&api_secret=${config.clientSecret}"

        val disposable = OkHttpHelper.get(requestUrl, "Content-Type" to "application/json")
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val jsonObject = it.createJSONObject()
                    val responseObject = jsonObject?.getJSONObject("response")

                    if (responseObject == null) {
                        responseFail(PlatformType.DISQUS)
                        return@subscribe
                    }

                    val avatarObject = responseObject.getJSONObject("avatar")
                    val profilePicture = avatarObject?.getJSONString("permalink") ?: ""

                    val item = LoginResultItem().apply {
                        this.id = responseObject.getJSONString("id")
                        this.name = responseObject.getJSONString("name")
                        this.email = responseObject.getJSONString("email")
                        this.nickname = responseObject.getJSONString("username")
                        this.profilePicture = profilePicture
                        this.platform = PlatformType.DISQUS
                        this.result = true
                    }

                    responseSuccess(item)
                }) {
                    responseFail(PlatformType.DISQUS)
                }

        compositeDisposable.add(disposable)
    }
}

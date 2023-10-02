package com.egorsigolaev.facebookadsdataextractor

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object FacebookDataExtractor {

    private lateinit var callback: Callback
    private lateinit var facebookDecryptionKey: String

    fun initialize(context: Context, facebookDecryptionKey: String, callback: Callback){
        this.callback = callback
        this.facebookDecryptionKey = facebookDecryptionKey.trim()
        initialize(context.applicationContext)
    }

    fun test(context: Context, installReferrer: String, facebookDecryptionKey: String, callback: Callback){
        this.callback = callback
        this.facebookDecryptionKey = facebookDecryptionKey
        try {
            val data = extractFacebookData(installReferrer)
            FacebookDataExtractor.callback.onSuccess(data)
        }catch (e: Error){
            FacebookDataExtractor.callback.onError(e)
        }catch (e: Exception){
            FacebookDataExtractor.callback.onError(Error.Unknown("Unknown error: ${e.message}"))
        }
    }

    private fun initialize(context: Context){
        if(facebookDecryptionKey.isEmpty()){
            callback.onError(Error.InvalidDecryptionKey("Facebook Install Referrer Decryption Key can't be empty"))
            return
        }
        if(facebookDecryptionKey.length % 2 != 0){
            callback.onError(Error.InvalidDecryptionKey("Facebook Install Referrer Decryption Key must have an even length"))
            return
        }
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        val installReferrer = referrerClient.installReferrer.installReferrer
                        try {
                            val data = extractFacebookData(installReferrer)
                            callback.onSuccess(data)
                        }catch (e: Error){
                            callback.onError(e)
                        }catch (e: Exception){
                            callback.onError(Error.Unknown("Unknown error: ${e.message}"))
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        callback.onError(Error.FeatureNotSupported)
                    }
                    else -> {
                        callback.onError(Error.Unknown("Referrer client error: $responseCode"))
                    }
                }
                referrerClient.endConnection()
            }

            override fun onInstallReferrerServiceDisconnected() {}
        })
    }

    private fun extractFacebookData(installReferrer: String): FacebookData?{
        val referrerParams = getReferrerParams(installReferrer)
        return if(referrerParams["utm_source"] == "apps.facebook.com" && referrerParams["utm_campaign"] == "fb4a"){
            referrerParams["utm_content"]?.let { content ->
                val contentObject = JSONObject(URLDecoder.decode(content, "UTF-8"))
                val source = contentObject.getJSONObject("source")
                val data = source.getString("data")
                val nonce = source.getString("nonce")
                val facebookInstallDataJson = decryptInstallDataJson(data, nonce)
                try {
                    val dataObject = JSONObject(facebookInstallDataJson)
                    return FacebookData(
                        adId = dataObject.getString("adgroup_id"),
                        adName = dataObject.getString("adgroup_name"),
                        adSetId = dataObject.getString("campaign_id"),
                        adSetName = dataObject.getString("campaign_name"),
                        campaignId = dataObject.getString("campaign_group_id"),
                        campaignName = dataObject.getString("campaign_group_name"),
                        accountId = dataObject.getString("account_id"),
                        platform = dataObject.optString("publisher_platform")
                    )
                }catch (e: JSONException){
                    throw Error.Unknown("Failed to extract Facebook data json: $facebookInstallDataJson")
                }
            }
        }else{
            null
        }
    }

    private fun getReferrerParams(installReferrer: String): Map<String, String>{
        val map = HashMap<String, String>()
        installReferrer.split("&").forEach {
            val parts = it.split("=")
            if(parts.size > 1){
                map[parts[0]] = parts[1]
            }
        }
        return map
    }

    private fun decryptInstallDataJson(data: String, nonce: String): String{
        val mKey = SecretKeySpec(facebookDecryptionKey.decodeHex(), "AES/GCM/NoPadding")
        val mNonce = IvParameterSpec(nonce.decodeHex())
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, mKey, mNonce)
        return String(c.doFinal(data.decodeHex()))
    }

    private fun String.decodeHex(): ByteArray {
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    sealed class Error(override val message: String? = null): Exception(message){
        data object FeatureNotSupported: Error()
        data class InvalidDecryptionKey(override val message: String) : Error(message)
        data class Unknown(override val message: String): Error(message)
    }

    data class FacebookData(
        val adId: String,
        val adName: String,
        val adSetId: String,
        val adSetName: String,
        val campaignId: String,
        val campaignName: String,
        val accountId: String,
        val platform: String?,
    )

    interface Callback{
        fun onSuccess(data: FacebookData?)
        fun onError(error: Error)
    }

}
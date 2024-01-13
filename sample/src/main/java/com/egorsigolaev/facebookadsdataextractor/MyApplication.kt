package com.egorsigolaev.facebookadsdataextractor

import android.app.Application
import android.util.Log

class MyApplication: Application() {

    private val TAG = "FacebookDataExtractor"

    override fun onCreate() {
        super.onCreate()
        initializeLibrary()
        testLibrary()
    }

    private fun initializeLibrary(){
        FacebookDataExtractor.initialize(
            this,
            "12e6b3b611d6d77381d47cf85f261d9fcada95227d371b8eeb4b46189ac2a37f",
            object : FacebookDataExtractor.Callback{
                override fun onSuccess(data: FacebookDataExtractor.FacebookData?) {
                    if(data == null){
                        // Install source is likely not Facebook Ads
                        Log.d(TAG, "Install source is not Facebook Ads")
                        return
                    }
                    Log.d(TAG, "Facebook install data: $data")
                }

                override fun onError(error: FacebookDataExtractor.Error) {
                    when(error){
                        FacebookDataExtractor.Error.FeatureNotSupported -> {
                            // Feature is not supported
                        }
                        is FacebookDataExtractor.Error.InvalidDecryptionKey -> {
                            // Invalid Facebook decryption key, check it again
                            Log.d(TAG, "Message: ${error.message}")
                        }
                        is FacebookDataExtractor.Error.Unknown -> {
                            // Unknown error
                            Log.d(TAG, "Message: ${error.message}")
                        }
                    }
                }
            })
    }

    private fun testLibrary(){
        FacebookDataExtractor.test(
            "utm_source=apps.facebook.com&utm_campaign=fb4a&utm_content=%7B%22app%22%3A614892783859055%2C%22t%22%3A1685638522%2C%22source%22%3A%7B%22data%22%3A%228d3ed7aa9779f2a479d221b34d1206448c1f813a9940c637ea14002b1db828e3869dba9c5b700cf5cddc5fe0903a9c650be651bfb087522997b380602130f661d29b4ab2f46669b1d81629d5092d2409af0e608bc1e039697319cba3bd0cbb9708d9aa0f6c4ebdf49892c920854eff6563e0c1b422173329b9e5d2057a9c8954dd1e3c766fcc9db887e12455446473c1038583f38121cd4197c2d273db406eb4d60a91b4ee941ea8b6d45c27f5dc70456e94fe631a1c5dc19c5e53e21f96f50beef8422b8eddcddfe04008397b914aebe462c40f3b373a78c1eda3a19051ed844acfed13cc81d79f9b4fa2a3fd26897052cc4aa892546ffa4832927f9c0bba452bd13c3d7beed915970bb409d5ce9b7777509453fd651a095a9c70e2476090cac6c56bdb25a40978532f95c058df623cd0bbe8ff136b74bcb420d348e49e710b702eeb9d1e87a8c221abeb446712e1e59c3eacfc208e42ae3ca444d7e61737d462dcdb774c122a0dc520052652e19823372e%22%2C%22nonce%22%3A%22d65685722f88f93e019002e4%22%7D%7D",
            "12e6b3b611d6d77381d47cf85f261d9fcada95227d371b8eeb4b46189ac2a37f",
            object : FacebookDataExtractor.Callback{
                override fun onSuccess(data: FacebookDataExtractor.FacebookData?) {
                    if(data == null){
                        // Install source is likely not Facebook Ads
                        return
                    }
                    Log.d(TAG, "Facebook install data: $data")


                }

                override fun onError(error: FacebookDataExtractor.Error) {
                    // Handle potential errors
                }
            })
    }

}
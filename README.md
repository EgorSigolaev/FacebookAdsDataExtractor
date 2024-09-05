[![](https://jitpack.io/v/EgorSigolaev/FacebookAdsDataExtractor.svg)](https://jitpack.io/#EgorSigolaev/FacebookAdsDataExtractor)

# FacebookAdsDataExtractor

Get rid of MMP for attributing Facebook Ads

## IMPORTANT: Publish your app in Google Play before implementing the library.
Facebook Install Referrer Decryption Key is required. Otherwise the library will not be able to get installs data. 
You can get it from the link https://developers.facebook.com/apps/your-app-id/settings/basic


## Add the library to the project

```groovy
allprojects {
  repositories {
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
    implementation "com.github.EgorSigolaev:FacebookAdsDataExtractor:latest.version"
}
```

## Sample

```kotlin
FacebookDataExtractor.initialize(
            this,
            "FACEBOOK DECRYPTION KEY",
            object : FacebookDataExtractor.Callback{
            override fun onSuccess(data: FacebookDataExtractor.FacebookData?) {
                if(data == null){
                    // Install source is likely not Facebook Ads
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
```

## Testing (with test install referrer)

```kotlin
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
                    /*
                    Output example:
                    FacebookData(
                        adId=23854855793500700,
                        adName=Ad1,
                        adSetId=23854855793420700,
                        adSetName=AdSet1,
                        campaignId=23854855793350700,
                        campaignName=Test Campaign,
                        accountId=969070227561957,
                        platform=instagram
                    )
                     */
                }

                override fun onError(error: FacebookDataExtractor.Error) {
                    // Handle potential errors
                }
            })
```

## Firebase setup example
This library can also be used with Firebase to attribute users from Facebook Ads and measure LTV, ad revenue, RR, etc.
1) Add Firebase analytics dependency
2) Create custom definitions that you want to use as its shown below
<img width="641" alt="Screenshot1" src="https://github.com/EgorSigolaev/FacebookAdsDataExtractor/assets/44138374/ab2eeedd-6a64-46dc-86aa-bdf6e5f8b0ab">

3) Firebase SDK sample. It's important to set user properties **BEFORE** Firebase SDK initialization. Disable auto initialization in your manifest.
```kotlin
object : FacebookDataExtractor.Callback{
                override fun onComplete(result: FacebookDataExtractor.Result) {
                    when(result){
                        is FacebookDataExtractor.Result.Success -> {
                            result.data?.let { facebookData ->
                                Log.d(TAG, "Facebook install data: $facebookData")
                                val analytics = FirebaseAnalytics.getInstance(context)
                                analytics.setUserProperty("fb_account_id", facebookData.accountId)
                                analytics.setUserProperty("fb_ad_id", facebookData.adId)
                                analytics.setUserProperty("fb_ad_name", facebookData.adId)
                                analytics.setUserProperty("fb_ad_set_id", facebookData.adSetId)
                                analytics.setUserProperty("fb_ad_set_name", facebookData.adSetName)
                                analytics.setUserProperty("fb_campaign_id", facebookData.campaignId)
                                analytics.setUserProperty("fb_campaign_name", facebookData.campaignName)
                            } ?: run {
                                Log.d(TAG, "Install source is not Facebook Ads")
                            }

                        }
                        is FacebookDataExtractor.Result.Error -> {
                            // Handle potential error
                        }
                    }
                    // Initialize Firebase SDK
                    FirebaseApp.initializeApp(this@MyApplication)
                }
            }
```

4) Adapty SDK sample.
```kotlin
object : FacebookDataExtractor.Callback{
                override fun onComplete(result: FacebookDataExtractor.Result) {
                    // Initialize Adapty SDK
                    Adapty.activate(applicationContext, AdaptyConfig.Builder("YOUR_SDK_KEY").build())
                    when(result){
                        is FacebookDataExtractor.Result.Success -> {
                            result.data?.let { facebookData ->
                                Log.d(TAG, "Facebook install data: $facebookData")
                                val profileParameters = AdaptyProfileParameters.Builder()
                                    .withCustomAttribute("fb_account_id", facebookData.accountId)
                                    .withCustomAttribute("fb_ad_id", facebookData.adId)
                                    .withCustomAttribute("fb_ad_name", facebookData.adId)
                                    .withCustomAttribute("fb_ad_set_id", facebookData.adSetId)
                                    .withCustomAttribute("fb_ad_set_name", facebookData.adSetName)
                                    .withCustomAttribute("fb_campaign_id", facebookData.campaignId)
                                    .withCustomAttribute("fb_campaign_name", facebookData.campaignName)
                                    .build()
                                Adapty.updateProfile(profileParameters){

                                }
                            } ?: run {
                                Log.d(TAG, "Install source is not Facebook Ads")
                            }

                        }
                        is FacebookDataExtractor.Result.Error -> {
                            // Handle potential error
                        }
                    }
                }
            }
```

5) RevenueCat SDK sample.
```kotlin
object : FacebookDataExtractor.Callback{
                override fun onComplete(result: FacebookDataExtractor.Result) {
                    when(result){
                        is FacebookDataExtractor.Result.Success -> {
                            result.data?.let { facebookData ->
                                Log.d(TAG, "Facebook install data: $facebookData")

                                Purchases.sharedInstance.setAttributes(mapOf(
                                    "fb_account_id" to facebookData.accountId,
                                    "fb_ad_id" to facebookData.adId,
                                    "fb_ad_name" to facebookData.adName,
                                    "fb_ad_set_id" to facebookData.adSetId,
                                    "fb_ad_set_name" to facebookData.adSetName,
                                    "fb_campaign_id" to facebookData.campaignId,
                                    "fb_campaign_name" to facebookData.campaignName,
                                ))
                                
                            } ?: run {
                                Log.d(TAG, "Install source is not Facebook Ads")
                            }

                        }
                        is FacebookDataExtractor.Result.Error -> {
                            // Handle potential error
                        }
                    }
                    // Initialize RevenueCat
                    Purchases.configure(PurchasesConfiguration.Builder(applicationContext, "YOUR_SDK_KEY").build())
                }
            }
```

package com.fatkrab.vision

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.CountDownTimer
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import com.appodeal.ads.Appodeal
import com.appodeal.ads.BannerCallbacks
import com.appodeal.ads.InterstitialCallbacks
import com.appodeal.ads.RewardedVideoCallbacks
import com.appodeal.ads.initializing.ApdInitializationCallback
import com.appodeal.ads.initializing.ApdInitializationError
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ClipData
import android.content.ClipboardManager

class MainActivity : AppCompatActivity() {

    private var mInterstitialAd: InterstitialAd? = null
    public var imagePath: String = null.toString()


    private var job: Job? = null
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1


        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
    private fun countdown(){
        object : CountDownTimer(10000, 1000){
            override fun onTick(millisUntilFinished : Long) {
                val button2: Button = findViewById(R.id.button2)
                val timeto = (millisUntilFinished/1000).toInt().toString()

                buttonoffuntiltimerdown()
                button2.text = "Please hold on for: $timeto seconds. Make sure you have a stable connection, though either data or wifi."

            }

            override fun onFinish() {
                val button2: Button = findViewById(R.id.button2)

                timesuptouchbutton()
                button2.text = "Press me to start generating free, high quality images!"
            }
        }.start()
    }

    fun buttonoffuntiltimerdown() {
        val button2: Button = findViewById(R.id.button2)

        button2.isEnabled = false
    }
    fun timesuptouchbutton(){
        val button2: Button = findViewById(R.id.button2)

        button2.isEnabled = true
    }
    fun disableAndHideMainActivityViews() {
        val button: Button = findViewById(R.id.button)
        val editText: EditText = findViewById(R.id.editText)
        val presetButton: ToggleButton =findViewById(R.id.presetButton)
        val presetButton2: ToggleButton =findViewById(R.id.presetButton2)
        val presetButton3: ToggleButton =findViewById(R.id.presetButton3)
        val presetButton4: ToggleButton =findViewById(R.id.presetButton4)

        button.isEnabled = false
        editText.isEnabled = false

        button.visibility = View.GONE
        editText.visibility = View.GONE
        presetButton.visibility = View.GONE
        presetButton2.visibility = View.GONE
        presetButton3.visibility = View.GONE
        presetButton4.visibility = View.GONE
    }

    private suspend fun generateImage(theme: String): String {
        val client = OkHttpClient()

        val json = JSONObject().apply {
            put("steps", 40)
            put("width", 1024)
            put("height", 1024)
            put("seed", 0)
            put("cfg_scale", 5)
            put("samples", 1)
            val textPrompts = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", theme)
                    put("weight", 1)
                })
                put(JSONObject().apply {
                    put("text", " ")
                    put("weight", -1)
                })
            }
            put("text_prompts", textPrompts)
        }

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image")
            .header("Accept", "application/json")
            .header("Authorization", "sk-vCpXQC4ZlbAFdtafncOZSVvlIgkhFuxDqk4dSVLrsBVsVR34")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseStr = response.body?.string()

        if (responseStr.isNullOrEmpty()) {
            Log.e("MainActivity", "Empty response from server")
            throw Exception("Empty response from server")
        }

        Log.d("MainActivity", "Response: $responseStr")

        if (!response.isSuccessful) {
            throw Exception("Failed to generate image: ${response.code}")
        }

        val responseJson = JSONObject(responseStr)
        val artifacts = responseJson.getJSONArray("artifacts")
        var imagePath = ""

        for (i in 0 until artifacts.length()) {
            val artifact = artifacts.getJSONObject(i)
            val seed = artifact.getString("seed")
            imagePath = File(cacheDir, "txt2img_$seed.jpg").absolutePath

            val base64Image = artifact.optString("base64")
            if (base64Image.isNullOrEmpty()) {
                Log.e("MainActivity", "No base64 image found in artifact")
                continue
            }

            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

            options.inSampleSize = calculateInSampleSize(options, 512, 512)
            options.inJustDecodeBounds = false
            val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

            bmp?.let {
                FileOutputStream(imagePath).use { outputStream ->
                    it.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                Log.d("MainActivity", "Generated image path: $imagePath")
            } ?: run {
                Log.e("MainActivity", "Failed to decode image bytes")
            }
        }

        return imagePath
    }

    fun enableAndShowMainActivityViews() {
        val button: Button = findViewById(R.id.button)
        val editText: EditText = findViewById(R.id.editText)
        val presetButton: ToggleButton =findViewById(R.id.presetButton)
        val presetButton2: ToggleButton =findViewById(R.id.presetButton2)
        val presetButton3: ToggleButton =findViewById(R.id.presetButton3)
        val presetButton4: ToggleButton =findViewById(R.id.presetButton4)

        button.isEnabled = true
        editText.isEnabled = true

        button.visibility = View.VISIBLE
        editText.visibility = View.VISIBLE
        presetButton.visibility = View.VISIBLE
        presetButton2.visibility = View.VISIBLE
        presetButton3.visibility = View.VISIBLE
        presetButton4.visibility = View.VISIBLE
    }

    fun scaleBitmap(bitmap: Bitmap, desiredWidth: Int, desiredHeight: Int): Bitmap {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height
        val width = desiredWidth
        val height = (desiredWidth / aspectRatio).roundToInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    lateinit var mAdView: AdView
    lateinit var imageContainer: RelativeLayout
    lateinit var imageView: ImageView
    lateinit var closeButton: Button
    private final var TAG = "MainActivity"

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        countdown()

        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            MobileAds.initialize(this@MainActivity) {}
        }

        Appodeal.initialize(this@MainActivity, "2030607ae61422c066a3b0c37076a7aad4fb91da2a235416", Appodeal.BANNER or Appodeal.INTERSTITIAL or Appodeal.REWARDED_VIDEO, object :ApdInitializationCallback {
            override fun onInitializationFinished(errors: List<ApdInitializationError>?) {

                Appodeal.show(this@MainActivity, Appodeal.BANNER_TOP)
            }
        })

        Appodeal.setBannerCallbacks(object : BannerCallbacks {
            override fun onBannerLoaded(height: Int, isPrecache: Boolean) {

            }
            override fun onBannerFailedToLoad() {

            }
            override fun onBannerShown() {

            }
            override fun onBannerShowFailed() {

            }
            override fun onBannerClicked() {

            }
            override fun onBannerExpired() {

            }
        })

        Appodeal.setInterstitialCallbacks(object : InterstitialCallbacks {
            override fun onInterstitialLoaded(isPrecache: Boolean) {

            }
            override fun onInterstitialFailedToLoad() {

            }
            override fun onInterstitialShown() {

            }
            override fun onInterstitialShowFailed() {

            }
            override fun onInterstitialClicked() {

            }
            override fun onInterstitialClosed() {

            }
            override fun onInterstitialExpired() {

            }
        })

        Appodeal.setRewardedVideoCallbacks(object : RewardedVideoCallbacks {
            override fun onRewardedVideoLoaded(isPrecache: Boolean) {

            }
            override fun onRewardedVideoFailedToLoad() {

            }
            override fun onRewardedVideoShown() {

            }
            override fun onRewardedVideoShowFailed() {

            }
            override fun onRewardedVideoClicked() {

            }
            override fun onRewardedVideoFinished(amount: Double, currency: String) {

            }
            override fun onRewardedVideoClosed(finished: Boolean) {

            }
            override fun onRewardedVideoExpired() {

            }
        })

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Internet available")
            }


            override fun onLost(network: Network) {
                Log.d(TAG, "Internet lost")
                Toast.makeText(this@MainActivity, "You need to be connected to generate images", Toast.LENGTH_LONG).show()
            }
        }

        job = CoroutineScope(Dispatchers.IO).launch {

        }

        val editText: EditText = findViewById(R.id.editText)
        val button: Button = findViewById(R.id.button)
        val closeButton: Button = findViewById(R.id.closeButton)
        val imageContainer: RelativeLayout = findViewById(R.id.imageContainer)
        val frameLayout: FrameLayout =findViewById(R.id.frameLayout)
        val errorTextView: TextView = findViewById(R.id.errorTextView)
        val imageView: ImageView = findViewById(R.id.imageView)
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val presetButton: ToggleButton = findViewById(R.id.presetButton)
        val presetButton2: ToggleButton = findViewById(R.id.presetButton2)
        val presetButton3: ToggleButton = findViewById(R.id.presetButton3)
        val button2: Button = findViewById(R.id.button2)
        val presetButton4: ToggleButton = findViewById(R.id.presetButton4)
        val reportTextView: TextView = findViewById(R.id.reportTextView)
        val reportButton: Button = findViewById(R.id.reportButton)
        val reportScreen: RelativeLayout = findViewById(R.id.reportScreen)
        val closeButton2: Button =findViewById(R.id.closeButton2)
        val copyEmailButton: Button = findViewById(R.id.copyEmailButton)
        val textiView4: TextView = findViewById(R.id.textView4)
        val reportMessageTextView: TextView = findViewById(R.id.reportMessageTextView)
        val copyImagePromptButton: Button = findViewById(R.id.copyImagePromptButton)

        reportButton.setOnClickListener{
            reportScreen.visibility = View.VISIBLE
            closeButton2.visibility = View.VISIBLE
            copyEmailButton.visibility = View.VISIBLE
            reportMessageTextView.visibility = View.VISIBLE
        }

        closeButton2.setOnClickListener {
            reportScreen.visibility = View.GONE
            closeButton2.visibility = View.GONE
            copyEmailButton.visibility = View.GONE
            reportMessageTextView.visibility = View.GONE
        }
        button2.setOnClickListener{
            frameLayout.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        frameLayout.visibility = View.GONE
                    }
                })
        }

        closeButton.setOnClickListener {
            imageView.visibility = View.GONE
            closeButton.visibility = View.GONE
            reportTextView.visibility = View.GONE
            reportButton.visibility = View.GONE
            textiView4.visibility = View.GONE
            enableAndShowMainActivityViews()
            imageContainer.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        imageContainer.visibility = View.GONE
                    }
                })
        }

        presetButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                presetButton2.isChecked = false
                presetButton3.isChecked = false
                presetButton4.isChecked = false
            }
        }

        presetButton2.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                presetButton.isChecked = false
                presetButton3.isChecked = false
                presetButton4.isChecked = false
            }
        }

        presetButton3.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                presetButton.isChecked = false
                presetButton2.isChecked = false
                presetButton4.isChecked = false
            }
        }
        presetButton4.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked){
                presetButton.isChecked = false
                presetButton2.isChecked = false
                presetButton3.isChecked = false
            }
        }


        button.setOnClickListener {
            if(editText.text contentEquals("")) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "You didn't give a prompt. please write one", Toast.LENGTH_LONG).show()
                    Toast.makeText(this@MainActivity, "in the text box given and try again!", Toast.LENGTH_SHORT).show()
                }
            }else {

                val theme: String = if (presetButton.isChecked) {
                    "A hd anime-styled digital artpiece of ${editText.text}"
                } else if (presetButton2.isChecked) {
                    "A hd photo of a graffiti portraying ${editText.text}"
                } else if (presetButton3.isChecked) {
                    "A 3d, whimsical, pixar-like three dimentional representation of ${editText.text}"
                } else if (presetButton4.isChecked) {
                    "A hyperrealistic, 3d image of a ${editText.text}"
                }
                else {
                    editText.text.toString()
                }

                Log.e("MainActivity", "Your image is being generated!")
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Your image is being generated!",
                        Toast.LENGTH_LONG
                    ).show()
                }

                val backgroundScope = CoroutineScope(Dispatchers.IO)
                backgroundScope.launch {
                    MobileAds.initialize(this@MainActivity) {}
                }

                Appodeal.show(this@MainActivity, Appodeal.BANNER_TOP)

                imageContainer.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .setDuration(500)
                        .setListener(null)
                }
                disableAndHideMainActivityViews()

                progressBar.visibility = View.VISIBLE


                job = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        imagePath = generateImage(theme.toString())

                        withContext(Dispatchers.Main) {
                            val image = BitmapFactory.decodeFile(imagePath)
                            val scaledImage = scaleBitmap(image, 412, 250)
                            imageView.setImageBitmap(scaledImage)

                            errorTextView.text = ""
                            progressBar.visibility = View.GONE
                            imageView.visibility = View.VISIBLE
                            reportTextView.visibility = View.VISIBLE
                            reportButton.visibility = View.VISIBLE
                            textiView4.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "MainActivity",
                            "Failed to generate image, make sure to have a internet connection and to not request a NSFW theme, so the image is sucessfully generated!",
                            e
                        )
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to generate image, make sure to have a",
                                Toast.LENGTH_LONG
                            ).show()
                            Toast.makeText(
                                this@MainActivity,
                                "internet connection and to not request a NSFW",
                                Toast.LENGTH_LONG
                            ).show()
                            Toast.makeText(
                                this@MainActivity,
                                "theme, so the image is sucessfully generated!",
                                Toast.LENGTH_LONG
                            ).show()
                            errorTextView.text =
                                "Failed to generate image, make sure to have a internet connection and to not request a NSFW theme, so the image is sucessfully generated!"
                            progressBar.visibility = View.GONE
                            errorTextView.visibility = View.VISIBLE
                        }
                    }
                }
                GlobalScope.launch {
                    delay(25000)
                    withContext(Dispatchers.Main) {
                        closeButton.visibility = View.VISIBLE
                    }
                }
            }
        }
        copyEmailButton.setOnClickListener {
            val textToCopy = "visionaiimagegen@gmail.com"

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val clip = ClipData.newPlainText("Copied Text", textToCopy)

            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Email adress copied!", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Feel free to send a email", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "to this adress, in order ", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "to report offensive content", Toast.LENGTH_SHORT).show()
        }
        copyImagePromptButton.setOnClickListener{
            val textToCopy = editText.text.toString()


            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val clip = ClipData.newPlainText("Copied Text", textToCopy)

            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Prompt Copied to clipboard!", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Please include it in the email,", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "along with the offensive image,", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "so that i may work better on ", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "keeping offensive content out", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "of this platform!", Toast.LENGTH_SHORT).show()
        }


        val handler = Handler()
    }
    override fun onResume() {
        super.onResume()
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onPause() {
        super.onPause()
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()

        closeButton.visibility = View.GONE
        imageView.visibility = View.GONE
        imageContainer.visibility = View.GONE

        mInterstitialAd = null
    }

}
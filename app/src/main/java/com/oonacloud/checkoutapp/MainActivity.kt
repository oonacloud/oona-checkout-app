package com.oonacloud.checkoutapp

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Window
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.jetbrains.anko.alert
import org.tinylog.Logger
import java.io.File
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


var result = ""
var lastAmount = ""

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main)
        button.setOnClickListener{
            launchApp()
        }

        //Logger.info{"Start oona pay"}
        val sdMain = File("/sdcard/Android/data/com.oonacloud.checkoutapp/")
        if (!sdMain.exists()) {
            try {
                sdMain.mkdirs()
            } catch (e: Exception) {
                Log.e("checkoutapp",e.toString())
            }
        }

        val manager = this.packageManager
        val info = manager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        textViewVersion.text = info.packageName + " " + info.versionName

        textViewResult.text = result
        editText.setText(lastAmount)

        var current = ""
        editText.addTextChangedListener(object: TextWatcher {

            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val stringText = s.toString()

                if((stringText != current) && (stringText.length > current.length)) {
                    try {
                        editText.removeTextChangedListener(this)

                        val locale: Locale = Locale.FRANCE
                        val currency = Currency.getInstance(locale)
                        val cleanString = stringText.replace("[${currency.symbol},.\\s*]".toRegex(), "")
                        val parsed = cleanString.toDouble()
                        val formatted = NumberFormat.getCurrencyInstance(locale).format(parsed / 100)

                        current = formatted
                        lastAmount = current

                        editText.setText(formatted)
                        editText.setSelection(formatted.length)
                        editText.addTextChangedListener(this)

                    } catch (e: Exception) {
                        Log.e("checkoutapp",e.toString())
                    }

                } else {
                    current = stringText
                }
            }
        })

        imageOona.setOnClickListener {
            val openURL = Intent(android.content.Intent.ACTION_VIEW)
            openURL.data = Uri.parse(getString(R.string.oonaURL))
            startActivity(openURL)
        }

    }


    private fun launchApp() {

        //foregroundApp(1,"com.oonacloud.mobile.oonaapp")

        val newUUID = UUID.randomUUID()
        val locale: Locale = Locale.FRANCE
        val currency = Currency.getInstance(locale)
        val amount =  editText.text.toString().replace("[${currency.symbol},.\\s*]".toRegex(), "")
        val parsed = amount.toDouble()
        val formatted = parsed / 100

        val json =
            "{  \"amount\": {    \"value\": $formatted,    \"currency\": \"$currency\"  },  \"paymentType\": \"PURCHASE\",    \"merchantTransactionID\": \"$newUUID \" }"
        Logger.info{"Start post Oona payment : $json"}
        val start = System.currentTimeMillis()
        result = post("http://localhost:6100/service/payment",json)
        lastAmount = ""
        Logger.info{"Result : $result"}
        val duration = (System.currentTimeMillis()-start)/1000.toDouble()
        Logger.info{"Duration merchantTransactionID $newUUID : $duration s"}

        foregroundApp(1000,"com.oonacloud.checkoutapp")

        /*
        // Will not be needed with the production version
        thread(start = true) {
            // Oona App takes back control at the end of the transaction ... issue 476
            foregroundApp(15000,"com.oonacloud.checkoutapp")
        }*/
    }

    private fun foregroundApp(sleep: Long,app: String) {
        Thread.sleep(sleep)
        Logger.info{"foregroundApp : $sleep milliseconds, $app"}
        val pm = applicationContext.packageManager.also {
            intent = it.getLaunchIntentForPackage(app)
        }
        intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent?.addCategory(Intent.CATEGORY_LAUNCHER)
        try {
            applicationContext.startActivity(intent)
        } catch (e: Exception) {
            alert("Unable to bring the Oona application to foreground $app => startActivity(intent) : $e") {
                title = "Alert"
                yesButton { }
                noButton { }
            }.show()
        }
    }


    private fun post(url: String, json: String): String {

        return try {
            val policy =
                StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            val mediaType: MediaType? = MediaType.parse("application/json;charset=utf-8")
            val client = OkHttpClient.Builder()
                .connectTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build()
            val body: RequestBody = RequestBody.create(mediaType, json)
            val request: Request = Request.Builder().url(url).post(body).build()
            val response: Response = client.newCall(request).execute()

            val responseHeaders = response.headers()
            var sHeaders = ""
            var i = 0
                val size: Int = responseHeaders.size()
                while (i < size) {
                    sHeaders += responseHeaders.name(i).toString() + ": " + responseHeaders.value(i) + "\n"
                    i++
                }

            return response.protocol().toString() + " " + response.code().toString() + " " + response.message().toString() + "\n\n" + sHeaders + "\n" + response.body()!!.string()

    } catch (e: Exception) {
        Logger.error{e.toString()}
        return e.toString()
    }

    }

}

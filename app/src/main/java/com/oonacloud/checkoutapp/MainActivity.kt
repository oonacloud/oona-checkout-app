package com.oonacloud.checkoutapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import org.jetbrains.anko.alert


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val packageName = "com.oonacloud.mockupapp"
        button.setOnClickListener{
            launchApp(packageName)
        }
    }

    private fun launchApp(packageName: String) {
        val pm = applicationContext.packageManager
        val intent:Intent? = pm.getLaunchIntentForPackage(packageName)
        intent?.addCategory(Intent.CATEGORY_LAUNCHER)
        try {
            applicationContext.startActivity(intent)
        } catch (e: Exception) {
            alert("Unable to bring the Oona application to foreground $packageName => startActivity(intent) : $e") {
                title = "Alert"
                yesButton { }
                noButton { }
            }.show()
        }
    }
}

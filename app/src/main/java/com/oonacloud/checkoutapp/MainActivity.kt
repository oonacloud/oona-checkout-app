package com.oonacloud.checkoutapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.net.Uri
import android.provider.Browser
import org.jetbrains.anko.alert


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val packageName = "com.oonacloud.mockupapp"

        // Click listener for button widget
        button.setOnClickListener{
            // Launch the app programmatically
            launchApp(packageName)
        }

    }


    // Custom method to launch an app
    private fun launchApp(packageName: String) {
        val pm = applicationContext.packageManager
        val intent:Intent? = pm.getLaunchIntentForPackage(packageName)
        intent?.addCategory(Intent.CATEGORY_LAUNCHER)

        try {
            applicationContext.startActivity(intent)
        } catch (e: Exception) {
            // Application was just removed?
            alert("Unable to bring the Oona application to foreground $packageName => startActivity(intent) : $e") {
                title = "Alert"
                yesButton { }
                noButton { }
            }.show()
        }
    }
}

// Extension function to show toast message
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
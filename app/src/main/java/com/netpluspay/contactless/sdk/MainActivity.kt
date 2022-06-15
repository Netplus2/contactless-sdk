package com.netpluspay.contactless.sdk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.netpluspay.contactless.sdk.start.ContactlessSdk

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ContactlessSdk.readContactlessCard(this, 10, "86CBCDE3B0A22354853E04521686863D", 10.0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let { inte ->
            inte.extras?.keySet()?.forEach {
                Log.e("tag", "key: $it , value: ${inte.getStringExtra(it)}")
            }
        }
    }
}
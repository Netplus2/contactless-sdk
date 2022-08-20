package com.netpluspay.contactless.sdk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.netpluspay.contactless.sdk.start.ContactlessSdk
import com.netpluspay.contactless.sdk.utils.ContactlessReaderResult

class MainActivity : AppCompatActivity() {

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tv: TextView = findViewById(R.id.hello_text)

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val data: Intent? = result.data
                if (result.resultCode == ContactlessReaderResult.RESULT_OK) {
                    data?.let { inte ->
                        Log.e("tag", "via result launcher")
                        Log.e("tag", "value: ${inte.getStringExtra("data")}")

                    }
                }
                if (result.resultCode == ContactlessReaderResult.RESULT_ERROR) {
                    data?.let { inte ->
                        Log.e("tag", "via result launcher")
                        Log.e("tag", "value: ${inte.getStringExtra("message")}")

                    }
                }
            }

        tv.setOnClickListener {
            ContactlessSdk.readContactlessCard(this,
                resultLauncher,
                "86CBCDE3B0A22354853E04521686863D",
                100000.0)
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        data?.let { inte ->
//            inte.extras?.keySet()?.forEach {
//                Log.e("tag", "key: $it , value: ${inte.getStringExtra(it)}")
//            }
//        }
//    }
}
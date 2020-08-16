package com.example.globallydynamic.minimal_sample.dynamicfeature

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jeppeman.globallydynamic.globalsplitcompat.GlobalSplitCompat

class DynamicActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        GlobalSplitCompat.installActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dynamic)
    }
}
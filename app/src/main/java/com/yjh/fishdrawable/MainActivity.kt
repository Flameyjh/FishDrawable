package com.yjh.fishdrawable

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val iv: ImageView = findViewById(R.id.iv_fish)
        iv.setImageDrawable(FishDrawable())
    }
}
package com.tserumula.dbcleanerforwhatsapp

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WebpageActivity : AppCompatActivity() {

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when( item.itemId ){
            android.R.id.home -> { //back button
                onBackPressed()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webpage)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) //shows back button
        supportActionBar?.elevation = 0F

        val source = intent.getStringExtra("source")

        if( source == "about" ){
            title = "About the Author"
        }

        val filename  = when( source ){
            "about" -> "about.html"
            else -> "about.html"
        }

        val htmlData = assets.open(filename).bufferedReader().use { it.readText() }
        val textview = findViewById<TextView>(R.id.web_view)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textview.text = Html.fromHtml( htmlData, Html.FROM_HTML_MODE_LEGACY)
        } else {
            textview.text = Html.fromHtml( htmlData)
        }

        textview.movementMethod = LinkMovementMethod.getInstance()

    }

}
package com.tserumula.dbcleanerforwhatsapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class RunHistory : AppCompatActivity() {
    private lateinit var listAdapter : ArrayAdapter<*>
    private var dataList = mutableListOf<String>()

    /* ____ OVERRIDE METHODS ___ */
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
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) //shows back button
        supportActionBar?.elevation = 0F

        val listView = findViewById<ListView>(R.id.listview)

        listAdapter = ArrayAdapter(
            applicationContext,
            R.layout.vlist,
            dataList
        )

        listView.adapter = listAdapter

        //Check log file and dump contents to listview
        val storage = this.getExternalFilesDir("/")
        if( storage != null ){
            val filename = storage.absolutePath + "/service_log.txt"
            val file = File(filename)
            if ( file.exists() && file.canRead() ){
                file.forEachLine {
                    val line = it.replace("run :","")
                    dataList.add(line)
                }
                if( dataList.size > 0 ){
                    listAdapter.notifyDataSetChanged()
                }
            }
        }

    }
}
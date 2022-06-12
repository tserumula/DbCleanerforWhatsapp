package com.tserumula.dbcleanerforwhatsapp

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity(){

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
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) //shows back button
        supportActionBar?.elevation = 0F

        supportFragmentManager.beginTransaction().replace(
            R.id.frame_container,
            SettingsFragment()
        ).commit()

    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }
}

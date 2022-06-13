package com.tserumula.dbcleanerforwhatsapp

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var listAdapter : ArrayAdapter<*>
    private var dataList = mutableListOf<String>()
    private var dataPaths = mutableListOf<String>()

    private lateinit var listAdapterWB : ArrayAdapter<*>
    private var dataListWB = mutableListOf<String>()
    private var dataPathsWB = mutableListOf<String>()

    private var settingAutoClean : String = "off"

    private fun checkStoragePermission( pName : String): Boolean{
        val permission = ContextCompat.checkSelfPermission( this.applicationContext, pName)
        return ( permission == PackageManager.PERMISSION_GRANTED )
    }

    private fun requestPermission( array : Array<String>, storageCode : Int) {

        ActivityCompat.requestPermissions( this,
            array,
            storageCode
        )
    }

    private fun permissionDenied(){
        val textview = findViewById<TextView>(R.id.view_permission_denied)
        val layout = findViewById<LinearLayout>(R.id.linear_layout_a)

        layout.visibility = View.INVISIBLE
        textview.visibility = View.VISIBLE

    }


    private fun isScheduleRunning(): Boolean{
        val intent = Intent(this, ScheduleReceiver::class.java)
        val isRunning =
            PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_CODE,
                intent,
                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)  {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
                } else {
                    PendingIntent.FLAG_NO_CREATE
                }
            ) != null
        return isRunning
    }

    private fun stopSchedule(){
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ScheduleReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)  {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }


    private fun setSchedule() {
        /*
        * Starts background service using alarm-manager
        * The broadcast is received by ScheduleReceiver.kt which activates ScheduleService.kt
        * */
        val calendar: Calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 3) // 3AM
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, 1) //To avoid firing the alarm immediately

        //calendar.timeInMillis = System.currentTimeMillis()
        //calendar.add(Calendar.SECOND, 3)
        val timeInMillis = calendar.timeInMillis

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ScheduleReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)  {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        //alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, 10000, pendingIntent)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        //alarmManager.set( AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        Toast.makeText(this, "Auto-clean up activated", Toast.LENGTH_SHORT).show()
    }

    private fun refreshViews(){
        val textNDView = findViewById<TextView>(R.id.not_detected_a)
        val textNDViewB = findViewById<TextView>(R.id.not_detected_b)

        val storage = this.getExternalFilesDir("/")
        if( storage != null ){
            val root = storage.parentFile?.parentFile?.parentFile?.parentFile
            if( root != null ) {
                val whatsAppFolder = root.absolutePath + "/WhatsApp/Databases"
                if ( File(whatsAppFolder).isDirectory) {
                    val files = File(whatsAppFolder).listFiles()
                    if( files != null && files.size > 1 && files.size - 1 > dataList.size ) {
                        for ( file in files ) {
                            val size = BigDecimal(( file.length() / 1e6)).setScale(2, RoundingMode.HALF_EVEN).toString()
                            if( !file.name.contains(MAIN_DBFILE_NAME) ) {
                                dataPaths.add(file.path)
                                dataList.add(file.name + "\n" + size + " MB")
                            }
                        }
                        listAdapter.notifyDataSetChanged()
                        textNDView.visibility = View.INVISIBLE
                    }else{
                        //Nothing to clear
                        textNDView.text = resources.getString(R.string.clear_nothing)
                    }
                }

                // Repeat for Whatsapp Business folder
                val whatsAppBusinessFolder = root.absolutePath + "/WhatsApp Business/Databases"
                if ( File(whatsAppBusinessFolder).isDirectory) {
                    val filesWB = File(whatsAppBusinessFolder).listFiles()
                    if( filesWB != null && filesWB.size > 1 && filesWB.size - 1 > dataListWB.size ) {
                        for ( file in filesWB ) {
                            val size = BigDecimal(( file.length() / 1e6)).setScale(2, RoundingMode.HALF_EVEN).toString()
                            if( !file.name.contains(MAIN_DBFILE_NAME) ) {
                                dataPathsWB.add(file.path)
                                dataListWB.add(file.name + "\n" + size + " MB")
                            }
                        }
                        listAdapterWB.notifyDataSetChanged()
                        textNDViewB.visibility = View.INVISIBLE
                    }else{
                        textNDViewB.text = resources.getString(R.string.clear_nothing)
                    }
                }
            }
        }

        if( dataList.size > 0 ){
            findViewById<Button>(R.id.clear_whatsapp_button).visibility = View.VISIBLE
        }else{
            textNDView.visibility = View.VISIBLE
            findViewById<Button>(R.id.clear_whatsapp_button).visibility = View.INVISIBLE
        }

        if( dataListWB.size > 0 ){
            findViewById<Button>(R.id.clear_business_button).visibility = View.VISIBLE
        }else{
            textNDViewB.visibility = View.VISIBLE
            findViewById<Button>(R.id.clear_business_button).visibility = View.INVISIBLE
        }
    }

    private fun updateStatusView(){
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val textviewTitle = findViewById<TextView>(R.id.auto_clean_title)
        val textviewInfo = findViewById<TextView>(R.id.auto_clean_info)
        val textviewScheduler = findViewById<TextView>(R.id.auto_clean_scheduler)
        val prefAutoClean = prefs.getString("auto_clean_preference", "off")
        val arrayEntries = resources.getStringArray(R.array.settings_auto_entries)
        var scheduleString = "Not Running"

        if( prefAutoClean != null ){
            settingAutoClean = prefAutoClean
        }

        val status = when(prefAutoClean){
            "off" -> "OFF"
            else -> "Enabled"
        }

        val index = when( prefAutoClean ){
            "off" -> 0
            "daily" -> 1
            "weekly" -> 2
            else -> 0
        }

        if( index > 0 ){
            val isRunning = isScheduleRunning()
            if( !isRunning ) {
                //start service
                setSchedule()
            }
            scheduleString = "Running"
        }

        textviewTitle.text = resources.getString(R.string.status_auto_clean, status )
        textviewInfo.text = resources.getString(R.string.status_clean_info, arrayEntries[index])
        textviewScheduler.text = resources.getString(R.string.status_clean_schedule,scheduleString)

    }


    /* ____ OVERRIDE METHODS ____ */

    //Toolbar menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //Add items to the action bar
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when( item.itemId ){

            R.id.menu_refresh -> { //Refresh options
                refreshViews()
                true
            }
            R.id.menu_settings -> { //Settings
                val intent = Intent( this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.menu_exit -> {
                this.finishAffinity() //close app
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    refreshViews()
                } else {
                    permissionDenied()
                }
                return
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateStatusView()

        val wListView = findViewById<ListView>(R.id.whatsapp_listview)
        val wBListView = findViewById<ListView>(R.id.whatsapp_business_listview)
        val buttonA = findViewById<Button>(R.id.clear_whatsapp_button)
        val buttonB = findViewById<Button>(R.id.clear_business_button)

        listAdapter = ArrayAdapter(
            this.applicationContext,
            R.layout.vlist,
            dataList
        )

        listAdapterWB = ArrayAdapter(
            this.applicationContext,
            R.layout.vlist,
            dataListWB
        )

        wListView.adapter = listAdapter
        wBListView.adapter = listAdapterWB

        buttonA.setOnClickListener{
            val alert = AlertDialog.Builder(this)
            val length = dataList.size
            var tempTotal : Long = 0
            for (i in 0 until dataPaths.size){
                val file = File(dataPaths[i])
                if( file.exists() ){
                    tempTotal += file.length()
                }
            }
            val totalSize = if( tempTotal/1e6 > 1024 ){
                BigDecimal(( tempTotal / 1e9)).setScale(2, RoundingMode.HALF_EVEN).toString() + " GB"
            }else{
                BigDecimal(( tempTotal/ 1e6)).setScale(2, RoundingMode.HALF_EVEN).toString() + " MB"
            }

            val alertTitle = resources.getString(R.string.alert_clear_title )
            val alertSummary = resources.getString(R.string.alert_clear_summary, length, totalSize)

            alert.setTitle( alertTitle)
            alert.setMessage( alertSummary)

            alert.setPositiveButton("YES") { _, _ ->
                for (i in 0 until dataPaths.size){
                    val file = File(dataPaths[i])
                    if( file.canWrite() ){
                        file.delete()
                    }
                }

                dataPaths.clear()
                dataList.clear()
                listAdapter.notifyDataSetChanged()
                Toast.makeText( this , "Files Cleared successfully", Toast.LENGTH_SHORT ).show()
                refreshViews()
            }

            alert.setNegativeButton("NO") { dialog, _ ->
                dialog.cancel()
            }

            alert.create()
            alert.show()
        }


        buttonB.setOnClickListener{
            val alert = AlertDialog.Builder(this)
            val length = dataListWB.size - 1
            var tempTotal : Long = 0
            for (i in 1 until dataPathsWB.size){
                val file = File(dataPathsWB[i])
                if( file.exists() ){
                    tempTotal += file.length()
                }
            }
            val totalSize = if( tempTotal/1e6 > 1024 ){
                BigDecimal(( tempTotal / 1e9)).setScale(2, RoundingMode.HALF_EVEN).toString() + " GB"
            }else{
                BigDecimal(( tempTotal/ 1e6)).setScale(2, RoundingMode.HALF_EVEN).toString() + " MB"
            }

            val alertTitle = resources.getString(R.string.alert_clear_title )
            val alertSummary = resources.getString(R.string.alert_clear_summary, length, totalSize)

            alert.setTitle( alertTitle)
            alert.setMessage( alertSummary)

            alert.setPositiveButton("YES") { _, _ ->
                for (i in 0 until dataPathsWB.size){
                    val file = File(dataPathsWB[i])
                    if( file.canWrite() ){
                        file.delete()
                    }
                }

                dataPathsWB.clear()
                dataListWB.clear()
                listAdapterWB.notifyDataSetChanged()
                Toast.makeText( this , "Files Cleared successfully", Toast.LENGTH_SHORT ).show()
                refreshViews()
            }

            alert.setNegativeButton("NO") { dialog, _ ->
                dialog.cancel()
            }

            alert.create()
            alert.show()
        }

        //Check if we have read and write permission for storage
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val permissionA = checkStoragePermission(permissions[0])
        val permissionB = checkStoragePermission(permissions[1])

        if( !permissionA || !permissionB ){
            requestPermission(permissions, STORAGE_PERMISSION_CODE )
        }else{
            refreshViews()
        }

    }


    override fun onResume() {
        super.onResume()
        //check if settings changed
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val setting = prefs.getString("auto_clean_preference", "off")
        if( setting != settingAutoClean ){
            updateStatusView()
        }
    }


}
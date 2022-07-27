package com.tserumula.dbcleanerforwhatsapp

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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

    private fun checkStoragePermission(): Boolean{
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ){
            return Environment.isExternalStorageManager()
        }else{
            val pA = ContextCompat.checkSelfPermission( applicationContext, permissions[0])
            val pB = ContextCompat.checkSelfPermission( applicationContext, permissions[1])
            return ( pA == PackageManager.PERMISSION_GRANTED && pB == PackageManager.PERMISSION_GRANTED )
        }
    }

    private fun requestPermission() {

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ){
            //Android 11, 12,..
            try{
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, STORAGE_PERMISSION_CODE)
            }catch(e : Exception){
                e.printStackTrace()
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, STORAGE_PERMISSION_CODE)
            }
        }else{
            ActivityCompat.requestPermissions( this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_CODE
            )
        }
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
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        //calendar.timeInMillis = System.currentTimeMillis()
        //calendar.add(Calendar.SECOND, 20)

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
        Toast.makeText(this, "Auto-clean activated", Toast.LENGTH_SHORT).show()
    }

    private fun refreshViews(){
        val textViewA = findViewById<TextView>(R.id.detected_view_a)
        val textViewB = findViewById<TextView>(R.id.detected_view_b)

        val textViewClearA = findViewById<TextView>(R.id.nothing_clear_a)
        val textViewClearB = findViewById<TextView>(R.id.nothing_clear_b)

        dataPaths.clear()
        dataList.clear()

        val storage = this.getExternalFilesDir("/")
        if( storage != null ){
            val root = storage.parentFile?.parentFile?.parentFile?.parentFile
            if( root != null ) {
                val whatsAppFolder = if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)  {
                    root.absolutePath + "/Android/media/com.whatsapp/WhatsApp/Databases"
                }else{
                    root.absolutePath + "/WhatsApp/Databases"
                }
                if ( File(whatsAppFolder).isDirectory) {
                    textViewA.text = resources.getString(R.string.app_detected)

                    val files = File(whatsAppFolder).listFiles()
                    if( files != null && files.size > 1 ) {
                        for ( file in files ) {
                            val size = BigDecimal(( file.length() / 1e6)).setScale(2, RoundingMode.HALF_EVEN).toString()
                            if( !file.name.contains(MAIN_DBFILE_NAME) ) {
                                //Add file paths to memory so we don't have to scan again later
                                    // (unless during refresh)
                                dataPaths.add(file.path)
                                dataList.add(file.name + "\n" + size + " MB")
                            }
                        }
                        listAdapter.notifyDataSetChanged()
                    }
                }

                // Repeat for Whatsapp Business folder
                val whatsAppBusinessFolder = if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)  {
                    root.absolutePath + "/Android/media/com.whatsapp.w4b/WhatsApp Business/Databases"
                }else{
                    root.absolutePath + "/WhatsApp Business/Databases"
                }
                if ( File(whatsAppBusinessFolder).isDirectory) {
                    textViewB.text = resources.getString(R.string.app_detected)

                    val filesWB = File(whatsAppBusinessFolder).listFiles()
                    if( filesWB != null && filesWB.size > 1 ) {
                        for ( file in filesWB ) {
                            val size = BigDecimal(( file.length() / 1e6)).setScale(2, RoundingMode.HALF_EVEN).toString()
                            if( !file.name.contains(MAIN_DBFILE_NAME) ) {
                                dataPathsWB.add(file.path)
                                dataListWB.add(file.name + "\n" + size + " MB")
                            }
                        }
                        listAdapterWB.notifyDataSetChanged()
                    }
                }
            }
        }

        if( dataList.size > 0 ){
            //There are files we can clear. Show clear button on screen
            textViewClearA.visibility = View.GONE
            findViewById<Button>(R.id.clear_whatsapp_button).visibility = View.VISIBLE

        }else{
            //Nothing to clear
            textViewClearA.visibility = View.VISIBLE
            findViewById<Button>(R.id.clear_whatsapp_button).visibility = View.INVISIBLE
        }

        if( dataListWB.size > 0 ){
            textViewClearB.visibility = View.GONE
            findViewById<Button>(R.id.clear_business_button).visibility = View.VISIBLE
        }else{
            //Nothing to clear
            textViewClearB.visibility = View.VISIBLE
            findViewById<Button>(R.id.clear_business_button).visibility = View.INVISIBLE
        }
    }

    private fun updateStatusView(){
        //This updates the status bar shown at the bottom of the screen
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
            //user enables auto-clean option
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
        //Menu options for the app at top right corner
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

            R.id.menu_history -> {
                val intent = Intent( this, RunHistory::class.java)
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
        //Handle permission change event for Android < 11
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if( grantResults.isNotEmpty() ) {
                    if ( grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                        refreshViews()
                    } else {
                        permissionDenied()
                    }
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ( requestCode == STORAGE_PERMISSION_CODE ) {
            //Handle permission change event for Android 11
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ){
                if( Environment.isExternalStorageManager()){
                    //All good. Permission granted
                    refreshViews()
                }else{
                    //user says no no.
                    permissionDenied()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wListView = findViewById<ListView>(R.id.whatsapp_listview)
        val wBListView = findViewById<ListView>(R.id.whatsapp_business_listview)
        val buttonA = findViewById<Button>(R.id.clear_whatsapp_button)
        val buttonB = findViewById<Button>(R.id.clear_business_button)

        //we initialize the listAdapters first before we deal with the UI
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

        //update bottom status bar
        updateStatusView()

        //Set the click listener on the <clear files> button for WhatsApp files
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

        //Similarly, Set the click listener WhatsApp Business clear button
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

        //Check if we have read and write permission on storage.
            //UI will be refreshed on permission change
        val permitted = checkStoragePermission()
        if( !permitted ){
            requestPermission()
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
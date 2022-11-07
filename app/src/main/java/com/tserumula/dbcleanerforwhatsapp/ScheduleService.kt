package com.tserumula.dbcleanerforwhatsapp

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*


class ScheduleService : Service() {

    private fun clearFiles(folder : String, name : String) : String {
        var totalCleared = 0L
        var totalSize = 0L
        var outputString = "Cleared 0 files"

        if (File(folder).isDirectory) {
            val files = File(folder).listFiles()
            if (files != null && files.size > 1) {
                for (file in files) {
                    if (!file.name.contains(MAIN_DBFILE_NAME) && file.canWrite()) {
                        totalSize += file.length()
                        totalCleared += 1
                        file.delete()
                    }
                }

                if (totalSize > 0) {
                    val size = if (totalSize / 1e6 > 1024) {
                        BigDecimal((totalSize / 1e9)).setScale(2, RoundingMode.HALF_EVEN)
                            .toString() + " GB"
                    } else {
                        BigDecimal((totalSize / 1e6)).setScale(2, RoundingMode.HALF_EVEN)
                            .toString() + " MB"
                    }
                    outputString = "Cleared $totalCleared files of $size [$name]"
                }
            }
        }

        return outputString
    }


    private fun logEvent(str : String ){
        /*
        Saves a log file to the user's external directory to record when the background
        service was run.
        This file is kept only for the users attention. It is not transmitted in any way.
         */
        val storage = this.getExternalFilesDir("/")
        if( storage != null ){
            val filename = storage.absolutePath + "/service_log.txt"
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.UK )
            val data =  "run :" + sdf.format( Date() ) +  ", $str \n"
            try {
                val fos = FileOutputStream( File(filename) , true )
                fos.bufferedWriter().use{
                    it.write(data)
                }
                fos.close()
            } catch (fileNotFound: FileNotFoundException) {
                Log.d("ScheduleService", "File not found exception")
            }
            catch (ioException: IOException) {
                Log.d("ScheduleService", "Input output exception")
            }
        }
    }

    private fun runAutoClean(cA : Boolean, cB : Boolean, s1 : String, s2 : String): String{
        var tempA = ""
        var tempB = ""
        if( cA ) {
            tempA = clearFiles(s1, "WhatsApp")
        }

        if( cB ) {
            tempB = clearFiles(s2, "WhatsApp Business")
        }

        val output = if ( tempA.length > 20 && tempB.length < 20){
            //cleared WA but not WB
            tempA
        }else if( tempB.length > 20 && tempA.length < 20 ){
            //cleared WB but not WA
            tempB
        }else if (tempA.length > 20 && tempB.length > 20 ){
            //cleared both
            "$tempA; $tempB"
        }else if( tempA.length > 10){
            //cleared none
            tempA
        }else{
            //cleared none
            tempB
        }

        return output
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("ScheduleService", "Service on destroy")

        SleepLock.instance.getMyWakeLock()?.release()

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("ScheduleService", "Service running")
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val selectedApps = prefs.getStringSet("auto_clean_apps", HashSet<String>())
        var clearWA = false
        var clearWB = false
        val index = when( prefs.getString("auto_clean_preference", "off")){
            "off" -> 0
            "daily" -> 1
            "weekly" -> 2
            else -> 0
        }

        if (selectedApps != null){
            if (selectedApps.contains("wa")){
                clearWA = true
            }

            if( selectedApps.contains("wb")){
                clearWB = true
            }
        }


        if( index > 0 ) {
            var log = ""
            val storage = this.getExternalFilesDir("/")
            if (storage != null) {
                val root = storage.parentFile?.parentFile?.parentFile?.parentFile
                if (root != null) {
                    val whatsAppFolder = if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)  {
                        root.absolutePath + "/Android/media/com.whatsapp/WhatsApp/Databases"
                    }else{
                        root.absolutePath + "/WhatsApp/Databases"
                    }

                    val whatsAppBusinessFolder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        root.absolutePath + "/Android/media/com.whatsapp.w4b/WhatsApp Business/Databases"
                    } else {
                        root.absolutePath + "/WhatsApp Business/Databases"
                    }

                    if (index == 2) {
                        //Check if Frida then run auto-clean
                        if( Calendar.DAY_OF_WEEK == Calendar.FRIDAY ) {
                            log = runAutoClean(
                                clearWA,
                                clearWB,
                                whatsAppFolder,
                                whatsAppBusinessFolder
                            )
                        }
                    } else {
                        // run auto clean
                        log = runAutoClean(clearWA, clearWB, whatsAppFolder,whatsAppBusinessFolder)
                    }
                    //Toast.makeText(this, "Service.onStart()", Toast.LENGTH_LONG).show()
                    logEvent(log)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

}

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

    private fun clearFiles() : String {
        var totalCleared = 0L
        var totalSize = 0L
        var outputString = "Cleared 0 files"
        val storage = this.getExternalFilesDir("/")
        if ( storage != null) {
            val root = storage.parentFile?.parentFile?.parentFile?.parentFile
            if (root != null) {
                val whatsAppFolder = if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)  {
                    root.absolutePath + "/Android/media/com.whatsapp/WhatsApp/Databases"
                }else{
                    root.absolutePath + "/WhatsApp/Databases"
                }
                if (File(whatsAppFolder).isDirectory) {
                    val files = File(whatsAppFolder).listFiles()
                    if ( files != null && files.size > 1) {
                        for (file in files) {
                            if ( !file.name.contains(MAIN_DBFILE_NAME) && file.canWrite() ) {
                                totalSize += file.length()
                                totalCleared += 1
                                file.delete()
                            }
                        }
                        if( totalSize > 0 ) {
                            val size = if( totalSize/1e6 > 1024 ){
                                BigDecimal(( totalSize / 1e9)).setScale(2, RoundingMode.HALF_EVEN).toString() + " GB"
                            }else{
                                BigDecimal(( totalSize/ 1e6)).setScale(2, RoundingMode.HALF_EVEN).toString() + " MB"
                            }
                            outputString = "Cleared $totalCleared files of $size [WhatsApp]"
                        }
                    }
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
        val index = when( prefs.getString("auto_clean_preference", "off")){
            "off" -> 0
            "daily" -> 1
            "weekly" -> 2
            else -> 0
        }
        if( index > 0 ){
            var log  = ""
            if( index == 2 ){
                //check if friday then run auto clean
                if( Calendar.DAY_OF_WEEK == Calendar.FRIDAY ) {
                    log = clearFiles()
                }
            }else{
                // run auto clean
                log = clearFiles()
            }
            //Toast.makeText(this, "Service.onStart()", Toast.LENGTH_LONG).show()
            logEvent( log )
        }

        return super.onStartCommand(intent, flags, startId)
    }

}

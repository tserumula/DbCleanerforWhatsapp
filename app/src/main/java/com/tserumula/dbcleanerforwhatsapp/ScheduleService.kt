package com.tserumula.dbcleanerforwhatsapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ScheduleService : Service() {

    private fun logEvent(){
        val storage = this.getExternalFilesDir("/")
        if( storage != null ){
            val filename = storage.absolutePath + "service_log.txt"
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.UK )
            val data =  "run :" + sdf.format( Date() ) + "\n"
            try {
                val fos = FileOutputStream( File(filename) , true )
                fos.bufferedWriter().use{
                    it.write(data)
                }
                fos.close()
            } catch (fileNotFound: FileNotFoundException) {
                Log.d("Alarm Service", "File not found exception")
            }
            catch (ioException: IOException) {
                Log.d("Alarm Service", "Input output exception")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("Alarm Service", "Service on destroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Alarm Service", "Service running")
        //Toast.makeText(this, "Service.onStart()", Toast.LENGTH_LONG).show()
        logEvent()
        return super.onStartCommand(intent, flags, startId)
    }

}

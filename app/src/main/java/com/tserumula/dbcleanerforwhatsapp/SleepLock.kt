package com.tserumula.dbcleanerforwhatsapp

import android.os.PowerManager
import android.os.PowerManager.WakeLock


internal class SleepLock  // can't make instance from outside... we want to have single instance
// we want that outside use method "getInstance" to be able to use the object
private constructor() {
    private var myobject: PowerManager.WakeLock? = null

    fun getMyWakeLock(): PowerManager.WakeLock?{
        return myobject
    }

    fun setMyWakeLock(obj: WakeLock?) {
        myobject = obj
    }

    companion object {
        private var dataObj: SleepLock? = null
        val instance: SleepLock
            get() {
                if (dataObj == null){
                    dataObj = SleepLock()
                }
                return dataObj!!
            }

    }
}
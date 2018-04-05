package net.gosecure.spotbugs

import org.apache.maven.plugin.logging.Log

import com.esotericsoftware.minlog.Log as MiniLog

open class LogWrapper() {

    companion object {
        var log: Log? = null
        fun setMaven(log:Log) {
            this.log = log
        }
    }

    open fun info(msg:String) {
        if (log != null) {
            log!!.info(msg)
        }
        else {
            MiniLog.info(msg);
        }
    }

    open fun warn(msg:String) {
        if (log != null) {
            log!!.warn(msg)
        }
        else {
            MiniLog.warn(msg);
        }
    }

    open fun error(msg:String) {
        if (log != null) {
            log!!.error(msg)
        }
        else {
            MiniLog.error(msg)
        }
    }

    open fun error(msg:String, e:Exception) {
        if (log != null) {
            log!!.error(msg,e)
        }
        else {
            MiniLog.error(msg,e)
        }
    }
}
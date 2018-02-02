package net.gosecure.spotbugs

import org.apache.maven.plugin.logging.Log

open class LogWrapper() {

    companion object {
        lateinit var log: Log
        fun setMaven(log:Log) {
            this.log = log
        }
    }

    open fun info(msg:String) {
        if (log != null) {
            log.info(msg)
        }
    }

    open fun warn(msg:String) {
        if (log != null) {
            log.warn(msg)
        }
    }

    open fun error(msg:String) {
        if (log != null) {
            log.error(msg)
        }
    }

    open fun error(msg:String, e:Exception) {
        if (log != null) {
            log.error(msg,e)
        }
    }
}
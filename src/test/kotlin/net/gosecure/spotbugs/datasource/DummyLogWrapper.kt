package net.gosecure.spotbugs.datasource

import net.gosecure.spotbugs.LogWrapper


class DummyLogWrapper : LogWrapper() {

    override fun info(msg:String) {
        basicPrintln(msg)
    }

    override fun warn(msg:String) {
        basicPrintln(msg)
    }

    override fun error(msg:String) {
        basicPrintln(msg)
    }

    override fun error(msg:String, e:Exception) {
        basicPrintln(msg)
    }

    private fun basicPrintln(msg:String) {
        println(msg)
    }
}
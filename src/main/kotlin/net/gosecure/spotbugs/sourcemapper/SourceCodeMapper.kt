package net.gosecure.spotbugs.sourcemapper

interface SourceCodeMapper {

    fun getSourceFile(className:String,line:Int):Pair<String,Int>?
}
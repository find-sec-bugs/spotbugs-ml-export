package net.gosecure.spotbugs.sourcemapper

import net.gosecure.spotbugs.LogWrapper
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class FileSourceCodeMapper(val classMappingInputStream:InputStream, val log: LogWrapper) : SourceCodeMapper {

    val mapping = HashMap<Pair<String,Int>,Pair<String,Int>>()

    init {

        loadClassMapping(classMappingInputStream)
    }

    fun loadClassMapping(classMappingFile: InputStream):Map<Pair<String,Int>,Pair<String,Int>> {


        for(line in classMappingFile.bufferedReader().lines()) {
            val parts = line.split(",")
            if(parts.size < 2) continue
            var classPart = parts[0].split(":")
            var filePart = parts[1].split(":")
            if(classPart.size < 2 || filePart.size < 2) {
                log.error("The Mapping is not properly formatted ($line)")
                continue
            }
            if(mapping.get(Pair(classPart[0],Integer.parseInt(classPart[1]))) != null) continue
            mapping.put(Pair(classPart[0],Integer.parseInt(classPart[1])), Pair(filePart[0],Integer.parseInt(filePart[1])))
        }
        return mapping
    }


    override fun getSourceFile(className: String, line: Int):Pair<String,Int>? {
        return mapping.get(Pair(className, line))
    }
}
package net.gosecure.spotbugs.sourcemapper

class JavaOnlySourceCodeMapper : SourceCodeMapper {

    override fun getSourceFile(className: String, line: Int): Pair<String, Int>? {
        val javaFile = className.replace(".","/")+".java"
        return Pair(javaFile,line)
    }

}
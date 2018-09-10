package net.gosecure.spotbugs

import java.io.File
import java.util.ArrayList



class FileSearchUtil {
    private var fileNameToSearch: String? = null
    private val result = ArrayList<File>()

    fun getFileNameToSearch(): String? {
        return fileNameToSearch
    }

    fun setFileNameToSearch(fileNameToSearch: String) {
        this.fileNameToSearch = fileNameToSearch
    }

    fun getResult(): List<File> {
        return result
    }


    fun searchDirectory(directory: File, fileNameToSearch: String) {

        setFileNameToSearch(fileNameToSearch)

        if (directory.isDirectory) {
            search(directory)
        } else {
            throw RuntimeException(directory.absoluteFile.toString() + " is not a directory!")
        }

    }

    private fun search(file: File) {

        if (file.isDirectory) {

            for (temp in file.listFiles()!!) {
                if (temp.isDirectory) {
                    search(temp)
                } else {
                    if (getFileNameToSearch() == temp.name) {
                        result.add(temp)
                    }
                }
            }
        }

    }
}
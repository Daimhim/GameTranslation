package com.zjkj.networklib

import com.alibaba.excel.EasyExcel
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FilenameFilter
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter
import kotlin.collections.HashMap

val sourceFile = File("*")
val temporaryFilesXlsx = File("${System.getProperty("user.dir")}${File.separator}temporaryFiles.xlsx")


fun main(args: Array<String>) {
//    decoding()
    coding()
}

fun decoding() {
    val rpyFilenameFilter = RpyFilenameFilter()
    val listFiles = sourceFile.listFiles(rpyFilenameFilter) ?: return
    if (temporaryFilesXlsx.exists()) {
        temporaryFilesXlsx.delete()
    }
    val temporaryList = mutableListOf<MutableList<String>>()
    listFiles.forEach {
        val bufferedReader = it.bufferedReader()
        var readLine = bufferedReader.readLine()
        var row = mutableListOf<String>()
        while (readLine != null) {
            if (readLine.endsWith(":") && !readLine.endsWith("strings:")) {
                readLine = readLine.replace("translate chinese ", "")
                row = mutableListOf()
                readLine = getMD5Str(readLine)
                row.add(readLine)
                readLine = bufferedReader.readLine()
                continue
            }
            if (readLine.startsWith("    #")
                && (readLine.endsWith("\"")
                        || readLine.endsWith("nointeract")
                        || readLine.endsWith("(multiple=3)")
                        || readLine.endsWith("(multiple=2)"))) {
                val indexOf = readLine.indexOf("\"")
                val lastIndexOf = readLine.lastIndexOf("\"")
                if (indexOf != -1
                    && lastIndexOf != -1
                    && !readLine.contains("{")
                    && !readLine.contains("}")) {
                    val element = readLine.substring(indexOf + 1, lastIndexOf)
                    row.add(element)
                    temporaryList.add(row)
                }
            }
            readLine = bufferedReader.readLine()
        }
        bufferedReader.close()
    }
    EasyExcel
        .write(temporaryFilesXlsx)
        .sheet("Sheet1")
        .doWrite(temporaryList)

}

fun coding() {
    val translationResult = HashMap<String, String>()
    val xssfWorkbook = XSSFWorkbook(temporaryFilesXlsx)
    val sheetAt = xssfWorkbook.getSheet("Sheet2")
    for (row in sheetAt) {
        val cell = row.getCell(0)
        cell.setCellType(CellType.STRING)
        val toString = cell.stringCellValue
            .replace("\u200B", "")
            .replace("*","")
            .replace(" ","")
        val cell1 = row.getCell(1)
        cell1.setCellType(CellType.STRING)
        translationResult[toString] = cell1.stringCellValue
    }
    translationResult.forEach { t, u ->
        println("--|$t|--")
    }
    val rpyFilenameFilter = RpyFilenameFilter()
    val listFiles = sourceFile.listFiles(rpyFilenameFilter) ?: return
    var temporaryKey = ""
    var secondaryKey = ""
    var temporaryList = mutableListOf<String>()
    var strings = ""
    var isStrings = false
    var line = -1
    var calculateData = ""
    var isCalculateData = false
    listFiles.forEach {
        val bufferedReader = it.bufferedReader()
        var readLine = bufferedReader.readLine()
        temporaryList = mutableListOf()
        line = 0
        while (readLine != null) {
            line++
            if (readLine.endsWith("strings:")) {
                isStrings = true
                temporaryList.add(readLine)
                readLine = bufferedReader.readLine()
                continue
            }
            if (readLine.startsWith("    old ") && isStrings) {
                temporaryList.add(readLine)
                strings = readLine.replace("    old ", "")
                readLine = bufferedReader.readLine()
                continue
            }
            if (readLine.startsWith("    new ") && isStrings) {
                temporaryList.add("    new $strings")
                strings = ""
                readLine = bufferedReader.readLine()
                continue
            }
            if (readLine.endsWith(":") && !readLine.endsWith("strings:")) {
                isStrings = false
                temporaryKey = readLine.replace("translate chinese ", "")
                temporaryKey = getMD5Str(temporaryKey)
                temporaryList.add(readLine)
                readLine = bufferedReader.readLine()
                continue
            }
            if (readLine.startsWith("    #") && readLine.endsWith("\"") && !isStrings) {
                isStrings = false
                val readLine1 = readLine
                if (readLine.contains("{")
                    || readLine.contains("}")){
                    isCalculateData = true
                    calculateData = readLine1.replace("#", "")
                }
                secondaryKey = readLine.replace("    # ", "")
                val indexOf = secondaryKey.indexOf("\"")
                if (indexOf == -1){
                    println("|secondaryKey|${it.name}|$secondaryKey|$line")
                }
                secondaryKey = secondaryKey.substring(0, indexOf)
                temporaryList.add(readLine)
                readLine = bufferedReader.readLine()
                continue
            }
            if (readLine.startsWith("    $secondaryKey") && !isStrings) {
                isStrings = false
                val s = translationResult[temporaryKey]
                if (s == null && !isCalculateData) {
                    println("|temporaryKey|${it.name}|$temporaryKey|${readLine}|$line|")
                }
                if (isCalculateData){
                    isCalculateData = false
                    temporaryList.add(calculateData)
                }else{
                    temporaryList.add(readLine.replace("\"\"","\"$s\""))
                }
                readLine = bufferedReader.readLine()
                continue
            }
            temporaryList.add(readLine)
            readLine = bufferedReader.readLine()
        }
        bufferedReader.close()
        val bufferedWriter = it.bufferedWriter()
        temporaryList.forEach { content ->
            bufferedWriter.write(content)
            bufferedWriter.newLine()
        }
        bufferedWriter.flush()
        bufferedWriter.close()
    }
}

fun insertName(name: String):String{
    val stringBuffer = StringBuffer()
    name.forEach {
        stringBuffer.append(it).append("*")
    }
    stringBuffer.deleteCharAt(stringBuffer.length-1)
    return stringBuffer.toString()
}

fun equals(a: CharSequence?, b: CharSequence?): Boolean {
    if (a == b) return true
    var length: Int = 0
    return if (a != null && b != null && a.length.also { length = it } == b.length) {
        if (a is String && b is String) {
            a == b
        } else {
            for (i in 0 until length) {
                if (a[i] != b[i]) return false
            }
            true
        }
    } else false
}

//fun getKey():String{
//
//}
class RpyFilenameFilter : FilenameFilter {
    override fun accept(dir: File?, name: String?): Boolean {
        return name?.endsWith(".rpy") == true
    }
}

/** 加密key  */
private var PASSWORD_ENC_SECRET = "xxxxxxx-20202020"
/** 加密算法 */
private const val KEY_ALGORITHM = "AES"
/** 字符编码 */
private val CHARSET = Charset.forName("UTF-8")
/** 加解密算法/工作模式/填充方式 */
private const val CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding"

/**
 * 对字符串加密
 * @param data  源字符串
 * @return  加密后的字符串
 */
fun String.encrypt(): String {
    val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
    val byteArray = PASSWORD_ENC_SECRET.toByteArray(CHARSET)
    val keySpec = SecretKeySpec(byteArray, KEY_ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(byteArray))
    val encrypted = cipher.doFinal(this.toByteArray(CHARSET))
    return Base64.getEncoder().encodeToString(encrypted)
}

/**
 * 对字符串解密
 * @param data  已被加密的字符串
 * @return  解密得到的字符串
 */
fun String.decrypt(): String {
    val encrypted = Base64.getDecoder().decode(this.toByteArray(CHARSET))
    val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
    val byteArray = PASSWORD_ENC_SECRET.toByteArray(CHARSET)
    val keySpec = SecretKeySpec(byteArray, KEY_ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(byteArray))
    val original = cipher.doFinal(encrypted)
    return String(original, CHARSET)
}

fun getMD5Str(string: String):String{
//    val instance = MessageDigest.getInstance("md5")
//    val digest = instance.digest(string.toByteArray())
//    val toString = DatatypeConverter.printHexBinary(digest).toUpperCase()
    return string
}
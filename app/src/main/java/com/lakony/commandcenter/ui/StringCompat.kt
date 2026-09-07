package com.lakony.commandcenter.ui

fun String.substringAfter(delimiter: String, missingDelimiterValue: String, ignoreCase: Boolean): String {
    val index = indexOf(delimiter, ignoreCase = ignoreCase)
    return if (index < 0) missingDelimiterValue else substring(index + delimiter.length)
}

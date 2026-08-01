package com.trippin.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun dateTime(millis: Long): String = dateTimeFormat.format(Date(millis))
    fun date(millis: Long): String = dateFormat.format(Date(millis))
    fun time(millis: Long): String = timeFormat.format(Date(millis))
    fun km(value: Float): String = "${"%.1f".format(value)} km"
    fun speed(value: Float): String = "${"%.0f".format(value)} km/h"
    fun percent(value: Float): String = "${"%.0f".format(value)}%"
    fun inr(value: Float): String = "₹${"%.2f".format(value)}"
    fun fuelEconomy(kmPerLitre: Float): String = "${"%.1f".format(kmPerLitre)} km/L"
}

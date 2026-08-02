package com.trippin.app.auto

import android.os.Build
import com.trippin.app.tracking.VehicleDataCache

object CarHardwareIds {
    fun resolve(): String {
        val carData = VehicleDataCache.get()
        if (carData.carMake != null || carData.carModel != null) {
            return listOfNotNull(
                carData.carMake?.replace(" ", "-"),
                carData.carModel?.replace(" ", "-"),
                carData.carYear?.toString()
            ).joinToString("_")
        }
        return "aa_${Build.MODEL}_${Build.DEVICE}"
    }
}

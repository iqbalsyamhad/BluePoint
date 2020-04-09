package com.syamhad.bluepoint.func

import android.location.Location

data class PinModel(
    var name: String,
    var address: String,
    var distance: Float,
    var location: Location
)
package com.iotp.hsvcolorpicker.event

import com.iotp.hsvcolorpicker.ColorEnvelope

interface HueColorChangedListener {
    fun onHueChanged(colorEnvelope: ColorEnvelope)
}
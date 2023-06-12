package com.iotp.hsvcolorpicker.event

import com.iotp.hsvcolorpicker.ColorEnvelope

interface OnSatChangedListener {
    fun onSatChange(colorEnvelope: ColorEnvelope?, fromUser: Boolean)
}
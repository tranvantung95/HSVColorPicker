package com.iotp.hsvcolorpicker.event

interface ColorPreviewChangedListener {
    fun onRGBChange(newColor: Int)

    fun onHexColorChange(hexColor: String?)
}
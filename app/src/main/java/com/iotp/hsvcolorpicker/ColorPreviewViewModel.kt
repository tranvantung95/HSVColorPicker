package com.iotp.hsvcolorpicker

import android.graphics.Color
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ColorPreviewViewModel : ViewModel() {

    val alphaValue = MutableLiveData<Int>()
    val redValue = MutableLiveData<Int>()
    val greenValue = MutableLiveData<Int>()
    val blueValue = MutableLiveData<Int>()
    val hexColor = MutableLiveData<String>()
    val color = MediatorLiveData<Int>().apply {
        addSource(alphaValue) {
            postValue(createColor())
        }
        addSource(redValue) {
            postValue(createColor())
        }
        addSource(greenValue) {
            postValue(createColor())
        }
        addSource(blueValue) {
            postValue(createColor())
        }
    }

    private fun createColor(): Int {
        return Color.argb(
            alphaValue.value?.toInt() ?: 0,
            redValue.value ?: 0,
            greenValue.value ?: 0,
            blueValue.value ?: 0
        )
    }

     fun createColorFromHex() {
        val color = Color.parseColor(hexColor.toString())
    }

    fun alphaValid(input: String): Boolean {
        val number = input.toInt()
        return number in 1..99
    }

    private fun hexColorValid(colorString: String): Boolean {
        return try {
            Color.parseColor(colorString)
            true
        } catch (iae: IllegalArgumentException) {
            false
        }
    }

    fun isValidHexColor(color: String): Boolean {
        val hexColorRegex = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")
        val result = hexColorRegex.matches(color) && hexColorValid(color)
        return result
    }

    class ColorPreviewViewModelFactory() : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ColorPreviewViewModel::class.java)) {
                return ColorPreviewViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}
package com.iotp.hsvcolorpicker

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.iotp.hsvcolorpicker.event.AlphaColorChangedListener
import com.iotp.hsvcolorpicker.event.HueColorChangedListener
import com.iotp.hsvcolorpicker.event.OnSatChangedListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.time.Duration.Companion.microseconds

class MainActivity : AppCompatActivity(), OnSatChangedListener, HueColorChangedListener,
    AlphaColorChangedListener {
    private val colorPreview: ColorPreview by lazy {
        findViewById(R.id.colorPreview);
    }

    val ss: ColorPreviewViewModel by viewModels()

    private val rootView : LinearLayout by lazy {
        findViewById(R.id.root)
    }
    private val hueView: HueView by lazy {
        findViewById(R.id.cpv_hueView);
    }
    private val alphaView: AlphaView by lazy {
        findViewById(R.id.cpv_alphaView);
    }
    private val satView: SatView by lazy {
        findViewById(R.id.cpv_satView);
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hueView.setOnColorChangedListener(this)
        alphaView.setOnColorChangedListener(this)
        satView.setOnColorChangedListener(this)
        colorPreview.onColorChange = {
            satView.setColor(it, false)
            hueView.setColor(it, false)
            alphaView.setColor(it, false)
        }
        colorPreview.setInitColor(ColorEnvelope(Color.parseColor("#000000")))
    }

    override fun onAlphaChanged(newColor: Int) {
        colorPreview.updateAlphaColor(ColorEnvelope(newColor))
    }

    override fun onHueChanged(hueColor: Float) {
        colorPreview.updateRedColor(ColorEnvelope(hueColor.toInt()))
        satView.updateHue(ColorEnvelope(hueColor.toInt()), false)
        alphaView.updateHue(hueColor.toInt(), false)
    }

    override fun onSatChange(colorEnvelope: ColorEnvelope?, fromUser: Boolean) {
        alphaView.updateAlpha(colorEnvelope!!.color)
        colorPreview.apply {
            updateAlphaColor(colorEnvelope)
            updateRedColor(colorEnvelope)
            updateGreenColor(colorEnvelope)
            updateBlueColor(colorEnvelope)
        }
    }
}
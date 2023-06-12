package com.iotp.hsvcolorpicker

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.CheckResult
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.iotp.hsvcolorpicker.utils.Constant
import com.iotp.hsvcolorpicker.utils.CustomLifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class ColorPreview @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val customLifecycleOwner: CustomLifecycleOwner by lazy {
        CustomLifecycleOwner()
    }
    private val hexFlow: MutableStateFlow<String>? by lazy {
        MutableStateFlow("")
    }
    private val viewModel: ColorPreviewViewModel by lazy {
        val factory = ColorPreviewViewModel.ColorPreviewViewModelFactory()
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!, factory)
            .get(ColorPreviewViewModel::class.java)
    }

    private val edtHexColor: AppCompatEditText by lazy {
        findViewById(R.id.hexColor)
    }

    private val edtRedColor: AppCompatEditText by lazy {
        findViewById(R.id.edtRed)
    }
    private val edtGreenColor: AppCompatEditText by lazy {
        findViewById(R.id.edtGreen)
    }
    private val edtBlueColor: AppCompatEditText by lazy {
        findViewById(R.id.edtBlue)
    }
    private val edtAlpha: AppCompatEditText by lazy {
        findViewById(R.id.edtAlpha)
    }
    private val colorPanelView: ColorPanelView by lazy {
        findViewById(R.id.panelView)
    }
    var onColorChange: ((Int) -> Unit)? = null

    var callBack = false

    private val alphaTextChange: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            if (s?.isNotEmpty() == true) {
                if (viewModel.alphaValid(s.toString())) {
                    val result = s.toString().toFloat().div(100).times(Constant.DEFAULT_ALPHA_VALUE)
                    viewModel.alphaValue.postValue(result.roundToInt())
                    callBack = true
                    edtAlpha.setSelection(s.length)
                }
            }
        }
    }

    private val redTextChange: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            if (!s.isNullOrEmpty()) {
                viewModel.redValue.postValue(s.toString().toInt())
                callBack = true
                edtRedColor.setSelection(s.length)
            }
        }
    }

    private val blueTextChange: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            if (!s.isNullOrEmpty()) {
                viewModel.blueValue.postValue(s.toString().toInt())
                callBack = true
                edtBlueColor.setSelection(s.length)
            }
        }

    }

    private val greenTextChange: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            if (!s.isNullOrEmpty()) {
                viewModel.greenValue.postValue(s.toString().toInt())
                callBack = true
                edtGreenColor.setSelection(s.length)
            }
        }
    }

    private val hexTextChange: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            val hex = "#${s.toString()}"
            if (viewModel.isValidHexColor(hex)) {
                viewModel.hexColor.postValue(hex)
                callBack = edtHexColor.isFocused
            }
        }
    }

    fun debounceHexColorSearch() {
        customLifecycleOwner.lifecycle.coroutineScope.launch {
            hexFlow
                ?.debounce(300)?.distinctUntilChanged()?.catch { e ->
                    Log.d("HexColorError", e.message.toString())
                }?.asLiveData()?.observe(customLifecycleOwner) { s ->
                    if (s.toIntOrNull() != null) {
                        val result =
                            s.toString().toFloat().div(100).times(Constant.DEFAULT_ALPHA_VALUE)
                        viewModel.alphaValue.postValue(result.roundToInt())
                        callBack = true
                        edtAlpha.setSelection(s.length)
                        Log.d("sss", s.toString())
                    }
                }
        }
    }

    init {
        setContentView(R.layout.cpv_color_preview)
        customLifecycleOwner.onCreate()
        findControl()
        addTextChangeListener()
        debounceHexColorSearch()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        observeData()
        customLifecycleOwner.onAppear()
    }

    override fun onDetachedFromWindow() {
        customLifecycleOwner.onDisappear()
        super.onDetachedFromWindow()
    }

    fun setInitColor(color: ColorEnvelope) {
        removeTextChangeListener()
        if (!edtHexColor.isFocused){
            edtHexColor.setText(color.hexCode.orEmpty())
        }
        edtAlpha.setText(color.formatAlphaValue().toString())
        edtAlpha.setSelection(edtAlpha.text?.length ?: 0)
        edtBlueColor.setText(color.blueColor.toString())
        edtBlueColor.setSelection(edtBlueColor.text?.length ?: 0)
        edtRedColor.setText(color.redColor.toString())
        edtRedColor.setSelection(edtRedColor.text?.length ?: 0)
        edtGreenColor.setText(color.greenColor.toString())
        edtGreenColor.setSelection(edtGreenColor.text?.length ?: 0)
        colorPanelView.color = color.color
        addTextChangeListener()
    }


    private fun updateRedColor(color: ColorEnvelope) {
        edtRedColor.removeTextChangedListener(redTextChange)
        edtRedColor.setText(color.redColor.toString())
        viewModel.redValue.postValue(color.redColor)
        callBack = false
        edtRedColor.addTextChangedListener(redTextChange)
    }

    private fun updateGreenColor(color: ColorEnvelope) {
        edtGreenColor.removeTextChangedListener(greenTextChange)
        edtGreenColor.setText(color.greenColor.toString())
        callBack = false
        viewModel.greenValue.postValue(color.greenColor)
        edtGreenColor.addTextChangedListener(greenTextChange)
    }

    private fun updateBlueColor(color: ColorEnvelope) {
        edtBlueColor.removeTextChangedListener(blueTextChange)
        edtBlueColor.setText(color.blueColor.toString())
        callBack = false
        viewModel.blueValue.postValue(color.blueColor)
        edtBlueColor.addTextChangedListener(blueTextChange)
    }

    private fun updateAlphaColor(color: ColorEnvelope) {
        edtAlpha.removeTextChangedListener(alphaTextChange)
        edtAlpha.setText(color.formatAlphaValue())
        callBack = false
        viewModel.alphaValue.postValue(color.alphaColor)
        edtAlpha.addTextChangedListener(alphaTextChange)
    }

    fun updateColor(color: ColorEnvelope) {
        updateAlphaColor(color)
        updateRedColor(color)
        updateGreenColor(color)
        updateBlueColor(color)
    }

    private fun findControl() {
        edtAlpha.filters = arrayOf(InputFilterMinMax(0, 100))
        edtRedColor.filters = arrayOf(InputFilterMinMax(0, 255))
        edtGreenColor.filters = arrayOf(InputFilterMinMax(0, 255))
        edtBlueColor.filters = arrayOf(InputFilterMinMax(0, 255))

    }

    private fun observeData() {
        findViewTreeLifecycleOwner()?.let { owner ->
            viewModel.color.observe(owner) {
                setInitColor(ColorEnvelope(it))
                if (callBack) {
                    onColorChange?.invoke(it)
                }
            }

            viewModel.hexColor.observe(owner) {
                val color = viewModel.parseHexColorToInt(it)
                //Color.parseColor(it)
                setInitColor(ColorEnvelope(color))
                if (callBack) {
                    onColorChange?.invoke(color)
                }
            }
        }
    }

    private fun addTextChangeListener() {
        edtAlpha.addTextChangedListener(alphaTextChange)
        edtRedColor.addTextChangedListener(redTextChange)
        edtGreenColor.addTextChangedListener(greenTextChange)
        edtBlueColor.addTextChangedListener(blueTextChange)
        edtHexColor.addTextChangedListener(hexTextChange)

    }

    private fun removeTextChangeListener() {
        edtAlpha.removeTextChangedListener(alphaTextChange)
        edtRedColor.removeTextChangedListener(redTextChange)
        edtGreenColor.removeTextChangedListener(greenTextChange)
        edtBlueColor.removeTextChangedListener(blueTextChange)
        edtHexColor.removeTextChangedListener(hexTextChange)
    }
}


fun ViewGroup.setContentView(@LayoutRes id: Int) {
    LayoutInflater.from(context).inflate(id, this, true)
}

@ExperimentalCoroutinesApi
@CheckResult
fun EditText.textChanges(): Flow<CharSequence?> {
    return callbackFlow<CharSequence?> {
        val listener = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                trySend(s)
            }
        }
        addTextChangedListener(listener)
        awaitClose { removeTextChangedListener(listener) }
    }.onStart { emit(text) }
}
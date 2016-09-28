package com.github.oryanmat.trellowidget.util.color

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import com.github.oryanmat.trellowidget.R
import com.larswerkman.holocolorpicker.ColorPicker
import kotlinx.android.synthetic.main.color_chooser.view.*

val DEFAULT_VALUE = Color.BLACK

/**
 * a Preference class for colors (using a color picker) in the PreferenceFragments/Activities
 */
class ColorPreference @JvmOverloads constructor(
        context: Context, attributeSet: AttributeSet,
        var color: Int = DEFAULT_VALUE,
        var copyFrom: ColorPreference? = null) : DialogPreference(context, attributeSet) {

    init {
        dialogLayoutResource = R.layout.color_chooser
    }

    fun repairSVBar(view: View, colorToFix: Int) {
        // This is a work-around for a defect in holocolorpicker when using black with an SVBar
        val hsv = FloatArray(3)
        Color.colorToHSV(colorToFix, hsv)
        if (hsv[1] == 0.0f && hsv[2] == 0.0f) {
            view.svbar.setValue(hsv[2])
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        with(view.color_picker) {
            addSVBar(view.svbar)
            addOpacityBar(view.opacitybar)
            color = getPersistedInt(DEFAULT_VALUE)
            oldCenterColor = getPersistedInt(DEFAULT_VALUE)
            onColorChangedListener = ColorPicker.OnColorChangedListener { this@ColorPreference.color = it }
            repairSVBar(view, color)
        }
        with(view.copyButton) {
            val copyTarget = copyFrom as? ColorPreference
            visibility = if (copyTarget != null) View.VISIBLE else View.INVISIBLE
            if (copyTarget is ColorPreference) {
                text = context.getString(R.string.pref_title_copy_from_card, copyTarget.title)
                setOnClickListener {
                    view.color_picker.color = copyTarget.color
                    repairSVBar(view, copyTarget.color)
                }
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            persistInt(color)
        }
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (restorePersistedValue) {
            color = this.getPersistedInt(DEFAULT_VALUE)
        } else {
            color = defaultValue as Int
            persistInt(color)
        }
    }

    override fun onGetDefaultValue(array: TypedArray, index: Int): Any {
        return array.getInteger(index, DEFAULT_VALUE)
    }
}
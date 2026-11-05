package com.tanqueo.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ColorRamp
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.tanqueo.MainActivity
import com.tanqueo.R
import com.tanqueo.domain.usecase.ObserveDashboard
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Km restantes para cualquier carátula: con arco de progreso o como texto corto. */
@AndroidEntryPoint
class RangeComplicationService : SuspendingComplicationDataSourceService() {
    @Inject lateinit var observeDashboard: ObserveDashboard

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
        build(request.complicationType, RangeComplicationContent.from(observeDashboard.current().estimate))

    /** Solo se ve en el selector de complicaciones de la carátula, como ejemplo. */
    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(type, RangeComplicationContent(text = "385", tankPercent = 76f, hasData = true))

    private fun build(type: ComplicationType, content: RangeComplicationContent): ComplicationData? {
        val description = plain(
            if (content.hasData) {
                getString(R.string.complication_description, content.text)
            } else {
                getString(R.string.complication_no_data)
            },
        )
        val unit = plain(getString(R.string.unit_km))
        return when (type) {
            ComplicationType.RANGED_VALUE ->
                RangedValueComplicationData.Builder(content.tankPercent, 0f, 100f, description)
                    .setText(plain(content.text))
                    .setTitle(unit)
                    .setColorRamp(ColorRamp(intArrayOf(VIOLET, BLUE), interpolated = true))
                    .setTapAction(openApp())
                    .build()
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(plain(content.text), description)
                    .setTitle(unit)
                    .setMonochromaticImage(
                        MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_gas)).build(),
                    )
                    .setTapAction(openApp())
                    .build()
            else -> null
        }
    }

    private fun plain(text: String) = PlainComplicationText.Builder(text).build()

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val VIOLET = 0xFF7C5CFF.toInt()
        const val BLUE = 0xFF4AC0FC.toInt()
    }
}

package com.djskott.emporiasolaredgewidget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.djskott.emporiasolaredgewidget.MainActivity
import com.djskott.emporiasolaredgewidget.R
import com.djskott.emporiasolaredgewidget.data.WidgetSnapshotStore
import com.djskott.emporiasolaredgewidget.model.BalanceColor
import com.djskott.emporiasolaredgewidget.model.WidgetSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EmporiaSolarEdgeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val snapshot = WidgetSnapshotStore(context).load()
        provideContent {
            WidgetContent(snapshot)
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(snapshot: WidgetSnapshot) {
        val (background, textColor) = colorsFor(snapshot.balanceColor)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(background, background))
                .padding(12.dp),
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.Horizontal.Start,
            ) {
                Box(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = "Solar vs Home",
                        style = TextStyle(
                            color = ColorProvider(textColor, textColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        ),
                    )
                    Box(
                        modifier = GlanceModifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        CircleIconButton(
                            imageProvider = ImageProvider(R.drawable.ic_refresh_widget),
                            contentDescription = "Refresh widget",
                            onClick = actionRunCallback<WidgetRefreshAction>(),
                            modifier = GlanceModifier.size(34.dp),
                            backgroundColor = ColorProvider(Color.White, Color.White),
                            contentColor = ColorProvider(Color(0xFF223440), Color(0xFF223440)),
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                if (!snapshot.configReady) {
                    Text(
                        text = snapshot.statusMessage,
                        style = TextStyle(
                            color = ColorProvider(textColor, textColor),
                            fontSize = 14.sp,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Button(
                        text = "Open Setup",
                        onClick = actionStartActivity<MainActivity>(),
                    )
                    return@Column
                }

                MetricRow("Solar", formatKw(snapshot.solarProductionKw), textColor)
                MetricRow("Load", formatKw(snapshot.homeLoadKw), textColor)

                Spacer(modifier = GlanceModifier.height(10.dp))

                Text(
                    text = formatNet(snapshot.netKw),
                    style = TextStyle(
                        color = ColorProvider(textColor, textColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = snapshot.statusMessage,
                    style = TextStyle(
                        color = ColorProvider(textColor, textColor),
                        fontSize = 13.sp,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(10.dp))
                Text(
                    text = formatTimestamp(snapshot.lastUpdatedEpochMs),
                    style = TextStyle(
                        color = ColorProvider(textColor, textColor),
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun MetricRow(label: String, value: String, textColor: Color) {
        Text(
            text = "$label  $value",
            style = TextStyle(
                color = ColorProvider(textColor, textColor),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            ),
        )
    }

    private fun colorsFor(balanceColor: BalanceColor): Pair<Color, Color> {
        return when (balanceColor) {
            BalanceColor.GREEN -> Color(0xFF1B5E20) to Color.White
            BalanceColor.YELLOW -> Color(0xFF7A5A00) to Color.White
            BalanceColor.RED -> Color(0xFF8B1E1E) to Color.White
            BalanceColor.NEUTRAL -> Color(0xFF263238) to Color.White
        }
    }

    private fun formatKw(value: Double?): String {
        return if (value == null) "--" else String.format("%.2f kW", value)
    }

    private fun formatNet(value: Double?): String {
        return if (value == null) "Net --" else String.format("Net %+,.2f kW", value)
    }

    private fun formatTimestamp(epochMillis: Long?): String {
        if (epochMillis == null) {
            return "Updated: --"
        }
        val instant = Instant.ofEpochMilli(epochMillis)
        val localTime = instant.atZone(ZoneId.systemDefault())
        return "Updated: ${TIME_FORMATTER.format(localTime)}"
    }

    companion object {
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d h:mm a")
    }
}

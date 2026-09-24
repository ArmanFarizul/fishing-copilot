package com.fishingcopilot.alerts

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.fishingcopilot.FishingCopilotApp
import com.fishingcopilot.MainActivity
import com.fishingcopilot.R
import com.fishingcopilot.bite.BiteTimeline
import com.fishingcopilot.bite.primeWindows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Prime-time alerts: an alarm 45 minutes before each prime window at the home spot. Uses an exact
 * alarm when the user allowed "Alarms & reminders"; otherwise a 10-minute window, the tightest the
 * system allows without that permission (docs: developer.android.com/develop/background-work/services/alarms/schedule).
 */
class GoldenAlerts(private val app: FishingCopilotApp) {
    private val alarms = app.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    /** Recomputes the next alert from stored data and (re)sets the alarm; cancels it when alerts are off. */
    suspend fun reschedule() {
        val settings = app.alertSettings.current()
        val spotId = app.profileRepository.profile.first()?.homeSpotId
        if (!settings.goldenAlerts || spotId == null) {
            alarms.cancel(alarmIntent())
            app.alertSettings.setScheduled(null)
            setBootReceiverEnabled(false)
            return
        }
        setBootReceiverEnabled(true)
        val now = System.currentTimeMillis()
        val (_, forecast) = app.biteForecaster.forecast(spotId, now) ?: return
        val plan = GoldenAlertPlanner.next(primeWindows(forecast.points, BiteTimeline.PRIME_THRESHOLD), now, settings.lastNotifiedStart)
        app.alertSettings.setScheduled(plan)
        // With no prime time in the next 24 hours, look again in 6 hours.
        val triggerAt = plan?.alertAt ?: (now + RECHECK_MILLIS)
        if (canScheduleExact()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarmIntent())
        } else {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, triggerAt - WINDOW_MILLIS / 2, WINDOW_MILLIS, alarmIntent())
        }
    }

    /** Called when the alarm fires: announce the window if it is still coming, then plan the next one. */
    suspend fun onAlarm() {
        val settings = app.alertSettings.current()
        val spotId = app.profileRepository.profile.first()?.homeSpotId
        if (settings.goldenAlerts && spotId != null) {
            val now = System.currentTimeMillis()
            app.biteForecaster.forecast(spotId, now)?.let { (spotName, forecast) ->
                val windows = primeWindows(forecast.points, BiteTimeline.PRIME_THRESHOLD)
                GoldenAlertPlanner.due(windows, now, settings.lastNotifiedStart)?.let { window ->
                    notify(
                        title = app.getString(R.string.notif_golden_title, ((window.start - now) / 60_000).toInt()),
                        text = app.getString(
                            R.string.notif_golden_text, spotName, clock(window.start), clock(window.end),
                            String.format(app.resources.configuration.locales[0], "%.1f", window.peak)
                        ),
                        settings = settings,
                        id = NOTIFICATION_ID
                    )
                    app.alertSettings.setLastNotified(window.start)
                }
            }
        }
        reschedule()
    }

    fun sendTest(settings: AlertSettings) {
        notify(app.getString(R.string.notif_test_title), app.getString(R.string.notif_test_text), settings, TEST_NOTIFICATION_ID)
    }

    /** Channel sound and vibration are fixed once created, so each combination has its own channel. */
    fun createChannels() {
        val manager = app.getSystemService(NotificationManager::class.java)
        val sound = "android.resource://${app.packageName}/${R.raw.golden_chime}".toUri()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        fun channel(id: String, name: Int, description: Int, withSound: Boolean, withVibration: Boolean) =
            NotificationChannel(id, app.getString(name), NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = app.getString(description)
                setSound(if (withSound) sound else null, if (withSound) attributes else null)
                enableVibration(withVibration)
                if (withVibration) vibrationPattern = VIBRATION_PATTERN
            }
        manager.createNotificationChannels(
            listOf(
                channel(AlertChannel.FULL.id, R.string.channel_golden_full_name, R.string.channel_golden_full_description, true, true),
                channel(AlertChannel.SOUND.id, R.string.channel_golden_sound_name, R.string.channel_golden_sound_description, true, false),
                channel(AlertChannel.VIBRATE.id, R.string.channel_golden_vibrate_name, R.string.channel_golden_vibrate_description, false, true)
            )
        )
    }

    private fun notify(title: String, text: String, settings: AlertSettings, id: Int) {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) return
        val channel = AlertChannel.of(settings.sound, settings.vibration)
        val open = PendingIntent.getActivity(
            app, 0, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(app, channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSilent(channel == AlertChannel.SILENT)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(app).notify(id, notification)
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        app, 0, Intent(app, GoldenAlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun setBootReceiverEnabled(enabled: Boolean) {
        app.packageManager.setComponentEnabledSetting(
            ComponentName(app, AlertsRescheduleReceiver::class.java),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun clock(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(app.resources.configuration.locales[0]))

    companion object {
        private const val NOTIFICATION_ID = 45
        private const val TEST_NOTIFICATION_ID = 46
        private const val WINDOW_MILLIS = 10 * 60_000L
        private const val RECHECK_MILLIS = 6 * 60 * 60_000L
        private val VIBRATION_PATTERN = longArrayOf(0, 400, 200, 400)
    }
}

/** Fires 45 minutes before prime time. */
class GoldenAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as FishingCopilotApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.goldenAlerts.onAlarm()
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are cleared on reboot and when exact-alarm access changes; set the next one again. */
class AlertsRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) return
        val app = context.applicationContext as FishingCopilotApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.goldenAlerts.reschedule()
            } finally {
                pending.finish()
            }
        }
    }
}

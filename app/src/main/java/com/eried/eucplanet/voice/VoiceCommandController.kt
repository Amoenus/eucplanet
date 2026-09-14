package com.eried.eucplanet.voice

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.eried.eucplanet.R
import com.eried.eucplanet.data.model.MetricCatalog
import com.eried.eucplanet.data.repository.SettingsRepository
import com.eried.eucplanet.data.repository.WheelRepository
import com.eried.eucplanet.service.TonePlayer
import com.eried.eucplanet.service.VoiceService
import com.eried.eucplanet.voice.VoiceAnswer.Answer
import com.eried.eucplanet.voice.VoiceVocabulary.Kind
import com.eried.eucplanet.voice.VoiceVocabulary.SpokenTerm
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One listening session, end to end: tone, microphone, match, spoken answer.
 *
 * The rules that decide what gets said live in [VoiceCommandSession] and are
 * unit tested. This is the part that cannot be: it holds the Android pieces,
 * turns the catalog into names in the rider's language, and reads the values.
 *
 * It never goes quiet. Every path out of here speaks something, because a
 * rider at speed who hears nothing cannot tell a misheard word from a broken
 * feature, and the blank CONSUMPTION tile is what that costs.
 */
@Singleton
class VoiceCommandController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val wheelRepository: WheelRepository,
    private val voiceService: VoiceService,
    private val tonePlayer: TonePlayer,
) {

    private companion object {
        /** Short and high, so it carries over wind and is over before the
         *  rider starts speaking. */
        const val PROMPT_HZ = 1320
        const val PROMPT_MS = 90
    }

    /** What the dashboard renders while a session runs. */
    sealed interface UiState {
        data object Idle : UiState
        data object Listening : UiState
        data class Heard(val text: String) : UiState
        data class Spoke(val text: String) : UiState
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var session: Job? = null

    /** True when the rider has granted the microphone. */
    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Open the microphone. Safe to call again while listening: the rider
     * pressing twice should not start a second recogniser on top of the first.
     */
    fun listen(listener: VoiceListener? = null) {
        if (session?.isActive == true) return
        session = scope.launch {
            val settings = settingsRepository.get()
            if (!settings.voiceCommands.enabled) return@launch
            val mic = listener ?: AndroidVoiceListener(
                context = context,
                languageTag = settings.voiceLocale.ifBlank { "en-US" },
            )
            _state.value = UiState.Listening
            when (settings.voiceCommands.prompt) {
                "beep" -> tonePlayer.playBeep(PROMPT_HZ, PROMPT_MS)
                "voice" -> voiceService.speak(context.getString(R.string.voice_listening))
                // "none": the tile going into its listening state is the cue.
            }
            mic.start()

            val deadline = System.currentTimeMillis() +
                settings.voiceCommands.windowSeconds * 1000L
            var heard: String? = null
            while (System.currentTimeMillis() < deadline) {
                when (val s = mic.state.value) {
                    is ListenState.Partial -> _state.value = UiState.Heard(s.text)
                    is ListenState.Final -> { heard = s.text; break }
                    is ListenState.Failed -> break
                    else -> {}
                }
                delay(60)
            }
            mic.stop()
            answer(heard, settings)
            // Leave the answer on screen briefly, then go quiet.
            delay(4000)
            _state.value = UiState.Idle
        }
    }

    /** Work out the reply and speak it. Never returns without saying something. */
    private fun answer(heard: String?, settings: com.eried.eucplanet.data.model.AppSettings) {
        val vocabulary = vocabulary()
        val onDashboard = settings.dashboardMetricOrder
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        if (heard.isNullOrBlank()) {
            speak(VoiceAnswer.notUnderstood(vocabulary, onDashboard), settings)
            return
        }
        _state.value = UiState.Heard(heard)
        val answer = VoiceCommandSession.answer(heard, vocabulary, onDashboard) { term ->
            reading(term)
        }
        speak(answer, settings)
    }

    /**
     * The current value for a term.
     *
     * Reports come back null so [speak] can hand them to VoiceService, which
     * already formats and localises every one of them. Metrics that are also
     * reports take the same route, which is why Speed and Battery sound like
     * the announcement a rider already knows rather than a bare number.
     */
    private fun reading(term: SpokenTerm): VoiceCommandSession.Reading? {
        if (term.kind == Kind.REPORT || term.key in REPORT_FOR_METRIC) return null
        val data = wheelRepository.wheelData.value
        val value = EXTRACTORS[term.key]?.invoke(data)
        return when {
            value == null -> VoiceCommandSession.Reading(null, VoiceAnswer.Reason.NO_DATA_YET)
            value.isNaN() -> VoiceCommandSession.Reading(null, VoiceAnswer.Reason.NO_DATA_YET)
            else -> VoiceCommandSession.Reading(formatNumber(value), null)
        }
    }

    private fun speak(answer: Answer, settings: com.eried.eucplanet.data.model.AppSettings) {
        val text = when (answer) {
            is Answer.Say -> "${answer.name}, ${answer.value}"
            is Answer.Unavailable -> context.getString(reasonRes(answer.reason), answer.name)
            is Answer.NotUnderstood -> context.getString(
                R.string.voice_answer_unknown,
                answer.examples.getOrElse(0) { "" },
                answer.examples.getOrElse(1) { "" },
            )
            is Answer.NeedsChoice -> context.getString(
                R.string.voice_answer_which,
                answer.names.getOrElse(0) { "" },
                answer.names.getOrElse(1) { "" },
            )
        }
        _state.value = UiState.Spoke(text)

        // A report says itself, through the formatter that already knows the
        // rider's units and language.
        val reportKey = when (answer) {
            is Answer.Say -> REPORT_FOR_METRIC[keyFor(answer.name)] ?: keyFor(answer.name)
            else -> null
        }
        val spokenAsReport = reportKey != null && voiceService.answerReport(
            report = reportKey,
            data = wheelRepository.wheelData.value,
            settings = settings,
        )
        if (!spokenAsReport) voiceService.speak(text)
    }

    private fun keyFor(name: String): String =
        vocabulary().firstOrNull { it.name == name }?.key ?: name

    /** The names a rider can say, in their language. */
    private fun vocabulary(): List<SpokenTerm> = VoiceVocabulary.build(
        metricNames = MetricCatalog.all.associate { it.key to context.getString(it.labelRes) },
        reportNames = REPORT_NAMES.mapValues { context.getString(it.value) },
        splitName = context.getString(R.string.voice_split_term),
    )

    private fun reasonRes(reason: VoiceAnswer.Reason): Int = when (reason) {
        VoiceAnswer.Reason.OFF_IN_SETTINGS -> R.string.voice_answer_off
        VoiceAnswer.Reason.UNSUPPORTED_BY_WHEEL -> R.string.voice_answer_unsupported
        VoiceAnswer.Reason.NO_DATA_YET -> R.string.voice_answer_nodata
        VoiceAnswer.Reason.NEEDS_SETUP -> R.string.voice_answer_setup
    }

    private fun formatNumber(v: Float): String =
        if (v == v.toInt().toFloat()) v.toInt().toString() else "%.1f".format(v)
}

/** Report key to its translated name. */
private val REPORT_NAMES = mapOf(
    "Speed" to R.string.report_speed,
    "Battery" to R.string.report_battery,
    "PhoneBattery" to R.string.report_phone_battery,
    "Temp" to R.string.report_temp,
    "PWM" to R.string.report_pwm,
    "Current" to R.string.report_current,
    "Power" to R.string.report_power,
    "Distance" to R.string.report_distance,
    "Recording" to R.string.report_recording,
    "Time" to R.string.report_time,
    "Navigation" to R.string.report_navigation,
)

/**
 * Metrics that already have a spoken report. Routing these through
 * VoiceService means Speed and Battery are read out with the rider's units
 * and in their language, exactly as the periodic announcement says them,
 * rather than as a bare number with no unit.
 */
private val REPORT_FOR_METRIC = mapOf(
    "SPEED" to "Speed",
    "BATTERY" to "Battery",
    "PHONE_BATTERY" to "PhoneBattery",
    "TEMPERATURE" to "Temp",
    "LOAD" to "PWM",
    "CURRENT" to "Current",
    "MOTOR_POWER" to "Power",
    "BATTERY_POWER" to "Power",
    "TRIP" to "Distance",
)

/**
 * Live values for the metrics with no spoken report of their own. Numbers
 * without units for now: the units live in the dashboard's per-key formatter,
 * and lifting them out is its own change.
 */
private val EXTRACTORS: Map<String, (com.eried.eucplanet.data.model.WheelData) -> Float?> = mapOf(
    "VOLTAGE" to { it.voltage },
    "MOTOR_TEMP" to { it.temperatures.firstOrNull() },
    "CONTROLLER_TEMP" to { it.temperatures.getOrNull(1) },
    "BATTERY_TEMP" to { it.temperatures.getOrNull(2) },
    "ODOMETER" to { it.totalDistance },
    "TRIP_METER" to { it.tripMeterKm },
    "PITCH" to { it.pitchAngle },
    "ROLL" to { it.rollAngle },
    "G_FORCE" to { it.gForce },
    "FORWARD_G" to { it.forwardGFromSpeed },
    "LATERAL_G" to { it.accelX },
    "TORQUE" to { it.torque },
    "PHASE_CURRENT" to { it.phaseCurrent },
    "BATTERY_1" to { it.battery1Percent },
    "BATTERY_2" to { it.battery2Percent },
    "BATTERY_ENVELOPE" to { it.batteryEnvelope },
    // A wheel with no sensor reads 0, which is not a pressure. hasTirePressure
    // is the wheel saying whether it has one at all, and the rider hears
    // "your wheel does not report tyre pressure" instead of "0".
    "TIRE_PRESSURE" to { if (it.hasTirePressure) it.tirePressureKpa else null },
    "WH_CONSUMED" to { it.whConsumed },
    "REGEN_WH" to { it.whRegen },
    "WH_PER_KM" to { it.whPerKmRecent },
    "RANGE_ESTIMATE" to { it.rangeKmEstimate },
    "GPS_ALTITUDE" to { it.gpsAltitudeM },
    "GPS_SPEED" to { it.gpsSpeedKmh.takeIf { v -> v >= 0f } },
    "DYN_SPEED_LIMIT" to { it.dynamicSpeedLimit },
    "DYN_CURRENT_LIMIT" to { it.dynamicCurrentLimit },
    "WHEEL_MAX_SPEED" to { it.wheelMaxSpeedKmh.takeIf { v -> v >= 0f } },
    "WHEEL_ALARM_SPEED" to { it.wheelAlarmSpeedKmh.takeIf { v -> v >= 0f } },
    "BT_RSSI" to { it.rssiDbm.toFloat().takeIf { v -> v != 0f } },
    "EXTERNAL_GPS_BATTERY" to { it.externalGpsBatteryPercent.toFloat().takeIf { v -> v >= 0f } },
)

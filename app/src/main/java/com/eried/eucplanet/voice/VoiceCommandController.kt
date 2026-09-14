package com.eried.eucplanet.voice

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val appNotifier: com.eried.eucplanet.util.AppNotifier,
    private val weatherRepository: com.eried.eucplanet.weather.WeatherRepository,
    private val navigationEngine: com.eried.eucplanet.nav.NavigationEngine,
    private val tripRepository: com.eried.eucplanet.data.repository.TripRepository,
    // A Provider, not the manager itself: FlicManager injects this
    // controller, and Dagger cannot build a cycle of two constructors.
    private val flicManager: javax.inject.Provider<com.eried.eucplanet.flic.FlicManager>,
) {

    private companion object {
        const val TAG = "VoiceCommand"
        /** Short and high, so it carries over wind and is over before the
         *  rider starts speaking. */
        /**
         * How long to wait for the recogniser to open before cueing anyway.
         *
         * It is normally ready in well under this. The cap is there so a
         * device that never reports ready still gets a cue and a window,
         * rather than a rider holding a button that does nothing.
         */
        const val READY_WAIT_MS = 1500L

        /** How still the words must be before the transcript shows them. */
        const val PARTIAL_SETTLE_MS = 500L

        /**
         * How long the system recogniser needs to let go.
         *
         * Only paid when replacing a session already listening, so a first
         * press is as quick as it ever was. Measured rather than guessed: a
         * replacement 50 milliseconds after the stop was still refused.
         */
        const val RECOGNISER_RELEASE_MS = 300L

        /** How long to wait for a yes. Short: it is one word. */
        const val CONFIRM_WINDOW_MS = 4000L

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

    private val _showVocabulary = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Emitted when the rider asks what they can say.
     *
     * Three spoken examples is what fits in an answer at speed; the whole list
     * is what they actually asked for, and a screen can hold it. Whichever
     * surface is in front of them opens it.
     */
    val showVocabulary: SharedFlow<Unit> = _showVocabulary.asSharedFlow()

    private var session: Job? = null

    /**
     * The microphone currently open, if any.
     *
     * Cancelling the session coroutine does not close a recogniser: it is an
     * Android object with its own lifetime, and the stop call lives in code
     * the cancellation skips. Held here so a second press can close the first
     * one before opening its own, which is the difference between "listening
     * again" and ERROR_RECOGNIZER_BUSY dressed up as another app stealing the
     * microphone.
     */
    private var activeMic: VoiceListener? = null

    /** The last finished ride, read before a match so `read` need not suspend. */
    private var lastTripSnapshot: com.eried.eucplanet.data.model.TripRecord? = null

    /** True when the rider has granted the microphone. */
    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Open the microphone. Safe to call again while listening: the rider
     * pressing twice should not start a second recogniser on top of the first.
     */
    /**
     * @param notify post a transient saying it is listening. The default,
     *   because most surfaces that start this - a Flic, a volume key, the
     *   watch, the HUD - show the rider nothing at all, and a microphone that
     *   opened silently is indistinguishable from a button that did nothing.
     *   The dashboard tile passes false: it lights up, and two cues for one
     *   press is noise.
     */
    fun listen(listener: VoiceListener? = null, notify: Boolean = true) {
        // Pressing again means "forget that, listen to this". It used to mean
        // nothing at all: the second press was swallowed while the first
        // session ran out its window, so a rider who fumbled the first
        // question had to wait for the app to finish not understanding it.
        session?.cancel()
        // Whether we are taking the microphone off ourselves. The system
        // recogniser does not hand it back the instant stop() returns, so the
        // replacement waits a moment rather than racing its predecessor and
        // being told the microphone is busy - which it then reported as
        // another app holding it, blaming a stranger for our own handover.
        val handingOver = activeMic != null
        activeMic?.stop()
        activeMic = null
        _state.value = UiState.Idle
        session = scope.launch {
            val settings = settingsRepository.get()
            // Every surface that can start listening goes through here, so the
            // check belongs here rather than in each of them. A rider pressing
            // a watch stem or a HUD button with the permission never granted
            // was getting the microphone opened and then a recogniser error
            // dressed up as something else holding it.
            if (listener == null && !hasMicPermission()) {
                val text = context.getString(
                    R.string.voice_answer_setup,
                    context.getString(R.string.voice_commands_title),
                )
                _state.value = UiState.Spoke(text)
                voiceService.speak(text)
                Log.i(TAG, "refused, no microphone permission")
                com.eried.eucplanet.diagnostics.DiagnosticsLogger.note(
                    "Voice: refused, no microphone permission"
                )
                delay(4000)
                _state.value = UiState.Idle
                return@launch
            }
            val mic = listener ?: AndroidVoiceListener(
                context = context,
                languageTag = VoiceLocaleTag.tag(settings.voiceLocale),
            )
            _state.value = UiState.Listening
            // Whatever the app was saying, it stops now. Otherwise the
            // recogniser spends the window listening to our own voice.
            voiceService.stopSpeaking()
            if (notify) appNotifier.post(context.getString(R.string.voice_listening))

            // Open the microphone before cueing the rider. The cue used to
            // come first, so a rider who answered it promptly spoke into a
            // recogniser that had not finished starting and lost the first
            // word, which is why a word as short as "help" rarely landed.
            if (handingOver) delay(RECOGNISER_RELEASE_MS)
            activeMic = mic
            mic.start()
            val readyBy = System.currentTimeMillis() + READY_WAIT_MS
            while (System.currentTimeMillis() < readyBy &&
                mic.state.value is ListenState.Preparing
            ) {
                delay(20)
            }
            when (settings.voiceCommands.prompt) {
                "beep" -> tonePlayer.playPrompt()
                "voice" -> voiceService.speak(context.getString(R.string.voice_listening))
                // "none": the tile going into its listening state is the cue.
            }

            // The window starts now, not at the press: the seconds a rider
            // sets are seconds they get to speak.
            val deadline = System.currentTimeMillis() +
                settings.voiceCommands.windowSeconds * 1000L
            var heard: String? = null
            var micBusy = false
            // Partials arrive a word at a time. Showing each one made the
            // transcript flicker through "what's", "what's the" and so on,
            // which is a lot of movement for a rider to read at speed. Wait
            // until the words stop changing, so what appears is a phrase.
            var pending: String? = null
            var pendingSince = 0L
            while (System.currentTimeMillis() < deadline) {
                when (val s = mic.state.value) {
                    is ListenState.Partial -> {
                        val now = System.currentTimeMillis()
                        if (s.text != pending) {
                            pending = s.text
                            pendingSince = now
                        } else if (now - pendingSince >= PARTIAL_SETTLE_MS &&
                            _state.value != UiState.Heard(s.text)
                        ) {
                            _state.value = UiState.Heard(s.text)
                        }
                    }
                    is ListenState.Final -> { heard = s.text; break }
                    is ListenState.Failed -> { micBusy = s.micUnavailable; break }
                    else -> {}
                }
                delay(60)
            }
            mic.stop()
            if (activeMic === mic) activeMic = null
            // A rider recording a video did not mumble, they are being told
            // the microphone is spoken for. Saying "I did not catch that"
            // there is the app blaming them for its own conflict.
            if (micBusy) sayMicUnavailable() else answer(heard, settings)
            // Leave the answer on screen briefly, then go quiet.
            delay(4000)
            _state.value = UiState.Idle
        }
    }

    /**
     * Answer a phrase without opening the microphone.
     *
     * Backs the tappable What can I say list: a rider picks a name and hears
     * exactly what asking for it would say, with their own wheel's values,
     * their own units and their own language. Rule 10 asks a preview to show
     * the real configuration, and this is the real path, with only the
     * acoustics left out.
     *
     * It is also the only way to exercise the whole chain on a device where
     * nobody can speak: an emulator, or a bench.
     */
    fun answerPhrase(phrase: String) {
        if (session?.isActive == true) return
        session = scope.launch {
            val settings = settingsRepository.get()
            _state.value = UiState.Heard(phrase)
            answer(phrase, settings)
            delay(4000)
            _state.value = UiState.Idle
        }
    }

    /**
     * The things the app knows that are not wheel readings.
     *
     * Each returns a finished sentence or null, and null means "nothing to
     * say yet" rather than an error: no route, no trips, no forecast.
     */
    private fun special(key: String, settings: com.eried.eucplanet.data.model.AppSettings): String? = when (key) {
        VoiceVocabulary.Special.CONNECTED -> {
            val name = wheelRepository.connectedDeviceName.value
            if (wheelRepository.connectionState.value ==
                com.eried.eucplanet.ble.ConnectionState.CONNECTED
            ) {
                context.getString(R.string.voice_special_connected, name.orEmpty())
            } else {
                context.getString(R.string.voice_special_not_connected)
            }
        }

        VoiceVocabulary.Special.UPTIME -> {
            val since = wheelRepository.connectedSinceMs.value
            if (since <= 0L) context.getString(R.string.voice_special_not_connected)
            else context.getString(
                R.string.voice_special_uptime,
                spokenDuration(System.currentTimeMillis() - since),
            )
        }

        VoiceVocabulary.Special.WEATHER -> weatherVerdict(settings)

        VoiceVocabulary.Special.DAYLIGHT -> daylightLeft()

        VoiceVocabulary.Special.NAV_NEXT -> navNext()

        VoiceVocabulary.Special.LAST_TRIP -> lastTrip(settings)

        // Handled before it gets here: this one speaks itself.
        VoiceVocabulary.Special.REPORT -> null

        else -> null
    }

    /**
     * Is it a good day to ride.
     *
     * The same RidabilityScore the dashboard panel and the widgets use, via
     * the one helper that reads the rider's own thresholds, so the spoken
     * verdict cannot disagree with the number on screen.
     */
    private fun weatherVerdict(settings: com.eried.eucplanet.data.model.AppSettings): String? {
        val hours = weatherRepository.forecast.value?.hours ?: return null
        val now = System.currentTimeMillis()
        val hour = hours.minByOrNull { kotlin.math.abs(it.timeMs - now) } ?: return null
        val b = com.eried.eucplanet.weather.WeatherScoring.scoreOf(hour, settings)
        val verdict = context.getString(
            when {
                b.score >= 3f -> R.string.voice_weather_great
                b.score >= 1f -> R.string.voice_weather_good
                b.score >= -1f -> R.string.voice_weather_ok
                b.score >= -3f -> R.string.voice_weather_poor
                else -> R.string.voice_weather_bad
            }
        )
        // One reason, the worst one. A list of everything wrong with the
        // afternoon is not what a rider standing at the door asked for.
        val reason = when {
            b.snow -> R.string.voice_weather_snow
            b.rain -> R.string.voice_weather_rain
            b.wind -> R.string.voice_weather_wind
            b.cold -> R.string.voice_weather_cold
            b.hot -> R.string.voice_weather_hot
            b.night -> R.string.voice_weather_night
            else -> null
        }
        return if (reason == null) verdict
        else context.getString(R.string.voice_special_weather, verdict, context.getString(reason))
    }

    /**
     * How long until the sun goes down, stepped rather than solved.
     *
     * SunCalc gives an elevation for a moment, so this walks forward in ten
     * minute steps until the sun is under the horizon. Ten minutes is finer
     * than anyone speaks a time to, and a whole day of steps is 144 cheap
     * trigonometric calls.
     */
    private fun daylightLeft(): String? {
        val f = weatherRepository.forecast.value ?: return null
        val now = System.currentTimeMillis()
        val sun = com.eried.eucplanet.weather.SunCalc
        if (sun.elevationDeg(now, f.lat, f.lon) <= 0.0) {
            return context.getString(R.string.voice_special_dark)
        }
        var t = now
        val end = now + 24L * 60 * 60 * 1000
        while (t < end) {
            t += 10L * 60 * 1000
            if (sun.elevationDeg(t, f.lat, f.lon) <= 0.0) {
                return context.getString(R.string.voice_special_daylight, spokenDuration(t - now))
            }
        }
        // The sun never sets here today, which happens where this app is used.
        return context.getString(R.string.voice_special_daylight_all_day)
    }

    /** What the navigation would say next, or that there is no route. */
    private fun navNext(): String {
        val nav = navigationEngine.navState.value
        if (!nav.active) return context.getString(R.string.voice_special_nav_none)
        val main = nav.primaryText.ifBlank { return context.getString(R.string.voice_special_nav_none) }
        val distance = nav.distanceText
        return if (distance.isBlank()) main
        else context.getString(R.string.voice_special_nav, main, distance)
    }

    /** The last finished ride, fetched before the match. */
    private fun lastTrip(settings: com.eried.eucplanet.data.model.AppSettings): String? {
        val trip = lastTripSnapshot ?: return context.getString(R.string.voice_special_no_trips)
        val unit = com.eried.eucplanet.util.Units.effectiveDistanceUnit(settings)
        val distance = "%.1f %s".format(
            com.eried.eucplanet.util.Units.distance(trip.distanceKm, unit),
            com.eried.eucplanet.util.Units.distanceUnit(unit),
        )
        val duration = spokenDuration((trip.endTime ?: trip.startTime) - trip.startTime)
        return context.getString(R.string.voice_special_last_trip, distance, duration)
    }

    /** Hours and minutes, spoken the way a rider would say them. */
    private fun spokenDuration(ms: Long): String {
        val totalMinutes = (ms / 60000L).coerceAtLeast(0L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> context.getString(
                R.string.voice_duration_h_m, hours.toString(), minutes.toString()
            )
            else -> context.getString(R.string.voice_duration_m, minutes.toString())
        }
    }

    /**
     * Carry out a spoken action.
     *
     * The state-aware pair is the reason this is not a straight key lookup:
     * a rider saying "lights on" means on, not "flip whatever they are", and
     * a toggle would turn them off when they were already lit. The wheel has
     * no separate on and off commands, so the state decides whether to send
     * anything at all.
     */
    private suspend fun runAction(key: String) {
        val data = wheelRepository.wheelData.value
        val catalogKey = when (key) {
            VoiceAction.LIGHT_ON -> if (data.lightOn) null else "LIGHT_TOGGLE"
            VoiceAction.LIGHT_OFF -> if (data.lightOn) "LIGHT_TOGGLE" else null
            VoiceAction.LOCK -> if (wheelRepository.locked.value) null else "LOCK_TOGGLE"
            VoiceAction.UNLOCK -> if (wheelRepository.locked.value) "LOCK_TOGGLE" else null
            else -> key
        }
        if (catalogKey == null) {
            // Already as asked. Saying so beats silence, and beats toggling.
            Log.i(TAG, "action $key already satisfied")
            return
        }
        flicManager.get().runAction(catalogKey)
        Log.i(TAG, "action $key ran as $catalogKey")
        com.eried.eucplanet.diagnostics.DiagnosticsLogger.note("Voice: did $key")
    }

    /**
     * Ask before doing, then listen for a yes.
     *
     * A second window rather than a dialog, because the whole point of this
     * feature is that the rider is not looking at the screen.
     */
    private suspend fun confirmAndRun(answer: Answer.Act, settings: com.eried.eucplanet.data.model.AppSettings) {
        val mic = AndroidVoiceListener(
            context = context,
            languageTag = VoiceLocaleTag.tag(settings.voiceLocale),
        )
        mic.start()
        val readyBy = System.currentTimeMillis() + READY_WAIT_MS
        while (System.currentTimeMillis() < readyBy && mic.state.value is ListenState.Preparing) {
            delay(20)
        }
        tonePlayer.playPrompt()
        val deadline = System.currentTimeMillis() + CONFIRM_WINDOW_MS
        var said: String? = null
        while (System.currentTimeMillis() < deadline) {
            when (val st = mic.state.value) {
                is ListenState.Final -> { said = st.text; break }
                is ListenState.Failed -> break
                else -> {}
            }
            delay(60)
        }
        mic.stop()
        val yes = context.getString(R.string.voice_yes_terms)
            .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val heardYes = said?.lowercase()?.let { spoken -> yes.any { spoken.contains(it) } } == true
        if (heardYes) {
            runAction(answer.key)
            _state.value = UiState.Spoke(answer.name)
            voiceService.speak(answer.name)
        } else {
            val text = context.getString(R.string.voice_action_cancelled)
            _state.value = UiState.Spoke(text)
            voiceService.speak(text)
            Log.i(TAG, "action ${answer.key} cancelled")
        }
    }

    /** Something else holds the microphone. Say so, rather than blame the rider. */
    private fun sayMicUnavailable() {
        val text = context.getString(R.string.voice_answer_mic_busy)
        _state.value = UiState.Spoke(text)
        voiceService.speak(text)
        Log.i(TAG, "said \"$text\" (mic busy)")
        com.eried.eucplanet.diagnostics.DiagnosticsLogger.note("Voice: said \"$text\" (mic busy)")
    }

    /** Work out the reply and speak it. Never returns without saying something. */
    private suspend fun answer(heard: String?, settings: com.eried.eucplanet.data.model.AppSettings) {
        val vocabulary = vocabulary()
        val onDashboard = settings.dashboardMetricOrder
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        if (heard.isNullOrBlank()) {
            speak(VoiceAnswer.notUnderstood(vocabulary, onDashboard), settings)
            return
        }
        _state.value = UiState.Heard(heard)
        // The matcher picks one term, but `read` cannot suspend, so the two
        // readings that need I/O are fetched before the match rather than
        // inside it. Both are a single value from a flow already in memory.
        lastTripSnapshot = runCatching { tripRepository.allTrips.first() }
            .getOrNull()?.filter { it.endTime != null }?.maxByOrNull { it.startTime }
        val answer = VoiceCommandSession.answer(
            heard = heard,
            vocabulary = vocabulary,
            onDashboard = onDashboard,
            read = { term -> readingBlocking(term, settings) },
            needsConfirm = { key -> key in CONFIRMED_ACTIONS },
        )
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
    /** What the session calls: everything suspending has already happened. */
    private fun readingBlocking(
        term: SpokenTerm,
        settings: com.eried.eucplanet.data.model.AppSettings,
    ): VoiceCommandSession.Reading? = readingOf(term, settings)

    private fun readingOf(
        term: SpokenTerm,
        settings: com.eried.eucplanet.data.model.AppSettings,
    ): VoiceCommandSession.Reading? {
        if (term.kind == Kind.SPECIAL) {
            // A whole sentence rather than a value and a unit, so it travels
            // the same road a report does.
            val text = special(term.key, settings)
            return if (text.isNullOrBlank()) {
                VoiceCommandSession.Reading(null, VoiceAnswer.Reason.NO_DATA_YET)
            } else {
                VoiceCommandSession.Reading(null, null, reportText = text)
            }
        }
        val data = wheelRepository.wheelData.value
        val connected = wheelRepository.connectionState.value ==
            com.eried.eucplanet.ble.ConnectionState.CONNECTED

        // A wheel that is not connected has no readings, only the zeroes a
        // fresh WheelData is born with. The report route will happily phrase
        // those as "temperature 0 degrees", which is the one thing this must
        // never do: a rider hearing a number believes it. Asked with no wheel,
        // the honest answer is that there is nothing yet.
        if (!connected && term.key !in OFF_WHEEL) {
            return VoiceCommandSession.Reading(null, VoiceAnswer.Reason.NO_DATA_YET)
        }

        // Some things the wheel simply does not have. That is not "not yet",
        // and telling a rider to wait for a sensor their wheel was built
        // without is worse than telling them it is missing.
        if (UNSUPPORTED[term.key]?.invoke(data) == true) {
            return VoiceCommandSession.Reading(null, VoiceAnswer.Reason.UNSUPPORTED_BY_WHEEL)
        }

        // Anything with a spoken report of its own borrows that sentence: it is
        // already in the rider's language and units, and it is the same wording
        // the periodic announcement uses, so asking for Battery sounds like the
        // app rather than like a different feature.
        val report = if (term.kind == Kind.REPORT) term.key else REPORT_FOR_METRIC[term.key]
        if (report != null) {
            val text = voiceService.reportText(report, data, settings)
            // A report with nothing to say is the wheel having sent nothing
            // yet, not a reason to stay quiet.
            return if (text.isNullOrBlank()) {
                VoiceCommandSession.Reading(null, VoiceAnswer.Reason.NO_DATA_YET)
            } else {
                VoiceCommandSession.Reading(null, null, reportText = text)
            }
        }
        val value = EXTRACTORS[term.key]?.invoke(data)
        return when {
            value == null -> VoiceCommandSession.Reading(null, VoiceAnswer.Reason.NO_DATA_YET)
            value.isNaN() -> VoiceCommandSession.Reading(null, VoiceAnswer.Reason.NO_DATA_YET)
            // The dashboard's own formatter, so a spoken value carries the same
            // unit in the same rider's units as the tile they would have read.
            else -> VoiceCommandSession.Reading(
                com.eried.eucplanet.data.model.MetricValueFormat.format(
                    key = term.key,
                    raw = value,
                    speedUnit = com.eried.eucplanet.util.Units.effectiveSpeedUnit(settings),
                    speedUnitLabel = com.eried.eucplanet.util.Units.speedUnit(
                        context, com.eried.eucplanet.util.Units.effectiveSpeedUnit(settings)
                    ),
                    tempUnit = com.eried.eucplanet.util.Units.effectiveTempUnit(settings),
                    tempUnitLabel = com.eried.eucplanet.util.Units.tempUnit(
                        com.eried.eucplanet.util.Units.effectiveTempUnit(settings)
                    ),
                    distanceUnit = com.eried.eucplanet.util.Units.effectiveDistanceUnit(settings),
                    pressureUnit = com.eried.eucplanet.util.Units.effectivePressureUnit(settings),
                ),
                null,
            )
        }
    }

    private fun speak(answer: Answer, settings: com.eried.eucplanet.data.model.AppSettings) {
        val text = when (answer) {
            is Answer.Say -> "${answer.name}, ${answer.value}"
            // Already a whole sentence, name included.
            is Answer.SayReport -> answer.text
            is Answer.Act -> if (answer.confirm) {
                context.getString(R.string.voice_action_confirm, answer.name)
            } else {
                answer.name
            }
            is Answer.Unavailable -> context.getString(reasonRes(answer.reason), answer.name)
            is Answer.NotUnderstood -> context.getString(
                R.string.voice_answer_unknown,
                answer.helpPhrase,
            )
            // The list goes on screen, so the spoken half says where to look
            // rather than reciting three of fifty names. Reading examples out
            // was the answer when there was nothing to show.
            is Answer.Examples -> context.getString(R.string.voice_answer_examples)
            is Answer.NeedsChoice -> context.getString(
                R.string.voice_answer_which,
                answer.names.getOrElse(0) { "" },
                answer.names.getOrElse(1) { "" },
            )
        }
        _state.value = UiState.Spoke(text)
        if (answer is Answer.Examples) _showVocabulary.tryEmit(Unit)

        // An action is the one answer that does something. It runs after the
        // sentence is chosen so the rider hears the acknowledgement and the
        // wheel acts at the same moment, rather than acting into silence.
        if (answer is Answer.Act && !answer.confirm) {
            scope.launch { runAction(answer.key) }
        }

        // One sentence, one way out. The report route used to live here and
        // was unreachable: it only ran for Answer.Say, and a report-backed
        // term never produced one. Building the sentence in reading() instead
        // means there is nothing left to choose between at this point.
        voiceService.speak(text)

        // The log already carries what was heard. Without what was said, a
        // rider reporting "it answered the wrong thing" leaves us guessing
        // whether the matcher picked the wrong term or the value was wrong.
        if (answer is Answer.Act && answer.confirm) {
            scope.launch { confirmAndRun(answer, settings) }
        }

        val via = if (answer is Answer.SayReport) "report" else "answer"
        Log.i(TAG, "said \"$text\" ($via)")
        com.eried.eucplanet.diagnostics.DiagnosticsLogger.note("Voice: said \"$text\" ($via)")
    }

    /** The names a rider can say, in their language. */
    private fun vocabulary(): List<SpokenTerm> = VoiceVocabulary.build(
        metricNames = MetricCatalog.all.associate { it.key to context.getString(it.labelRes) },
        reportNames = REPORT_NAMES.mapValues { context.getString(it.value) },
        splitName = context.getString(R.string.voice_split_term),
        helpPhrases = context.getString(R.string.voice_help_terms),
        actionPhrases = mapOf(
            VoiceAction.LIGHT_ON to context.getString(R.string.voice_act_light_on_terms),
            VoiceAction.LIGHT_OFF to context.getString(R.string.voice_act_light_off_terms),
            VoiceAction.LOCK to context.getString(R.string.voice_act_lock_terms),
            VoiceAction.UNLOCK to context.getString(R.string.voice_act_unlock_terms),
            "HORN" to context.getString(R.string.voice_act_horn_terms),
            "RECORD_START" to context.getString(R.string.voice_act_record_start_terms),
            "RECORD_STOP" to context.getString(R.string.voice_act_record_stop_terms),
            "RESET_TRIP" to context.getString(R.string.voice_act_reset_trip_terms),
        ),
        specialPhrases = mapOf(
            VoiceVocabulary.Special.WEATHER to context.getString(R.string.voice_sp_weather_terms),
            VoiceVocabulary.Special.DAYLIGHT to context.getString(R.string.voice_sp_daylight_terms),
            VoiceVocabulary.Special.CONNECTED to context.getString(R.string.voice_sp_connected_terms),
            VoiceVocabulary.Special.UPTIME to context.getString(R.string.voice_sp_uptime_terms),
            VoiceVocabulary.Special.NAV_NEXT to context.getString(R.string.voice_sp_nav_terms),
            VoiceVocabulary.Special.LAST_TRIP to context.getString(R.string.voice_sp_last_trip_terms),
            VoiceVocabulary.Special.REPORT to context.getString(R.string.voice_sp_report_terms),
        ),
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
/**
 * Terms that mean something with no wheel connected.
 *
 * The clock, the phone's own battery, whether a trip is recording, where the
 * navigation is going: none of these come off the wheel, so refusing them
 * while disconnected would be refusing a question the app can answer.
 * Everything else is a reading, and a reading with no wheel is a zero
 * pretending to be a measurement.
 */
/**
 * Actions worth asking about before doing.
 *
 * Only the ones that throw something away. Stopping a recording ends a ride
 * that cannot be resumed, and a misheard word must not be able to do that.
 * Lights, the horn and the lock cost nothing to undo, so confirming them
 * would make the feature tiring for no safety gained.
 */
/**
 * Action keys as the voice layer names them.
 *
 * Distinct from the catalog's keys because two of them have no catalog
 * equivalent: the wheel has one lights command and one lock command, and
 * "lights on" is a statement about the result rather than a request to flip.
 */
private object VoiceAction {
    const val LIGHT_ON = "V_LIGHT_ON"
    const val LIGHT_OFF = "V_LIGHT_OFF"
    const val LOCK = "V_LOCK"
    const val UNLOCK = "V_UNLOCK"
}

private val CONFIRMED_ACTIONS = setOf("RECORD_STOP", "RESET_TRIP")

internal val OFF_WHEEL = setOf(
    "Time", "PhoneBattery", "Recording", "Navigation",
    "PHONE_BATTERY", "GPS_ALTITUDE", "GPS_SPEED", "EXTERNAL_GPS_BATTERY",
    VoiceVocabulary.HELP_KEY,
    // The specials are mostly about the world rather than the wheel, and
    // "is the wheel connected" is asked precisely when it is not.
    VoiceVocabulary.Special.WEATHER, VoiceVocabulary.Special.DAYLIGHT,
    VoiceVocabulary.Special.CONNECTED, VoiceVocabulary.Special.UPTIME,
    VoiceVocabulary.Special.NAV_NEXT, VoiceVocabulary.Special.LAST_TRIP,
    VoiceVocabulary.Special.REPORT,
)

/**
 * Metrics a wheel can be built without, and how to tell.
 *
 * Distinct from having no value yet: the rider should stop waiting rather
 * than ask again in a minute.
 */
private val UNSUPPORTED: Map<String, (com.eried.eucplanet.data.model.WheelData) -> Boolean> =
    mapOf("TIRE_PRESSURE" to { !it.hasTirePressure })

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

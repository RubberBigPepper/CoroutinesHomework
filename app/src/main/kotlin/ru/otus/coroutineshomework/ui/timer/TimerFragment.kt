package ru.otus.coroutineshomework.ui.timer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.emptyLongSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.otus.coroutineshomework.databinding.FragmentTimerBinding
import java.util.Calendar
import java.util.Locale
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private var timerJob: Job? = null

    private var timeFlow: MutableStateFlow<Duration> = MutableStateFlow(ZERO)

    private var started by Delegates.observable(false) { _, _, newValue ->
        setButtonsState(newValue)
        if (newValue) {
            startTimer()
        } else {
            stopTimer()
        }
    }

    private fun setButtonsState(started: Boolean) {
        with(binding) {
            btnStart.isEnabled = !started
            btnStop.isEnabled = started
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            timeFlow = MutableStateFlow(it.getLong(TIME).milliseconds)
            started = it.getBoolean(STARTED)
        }
        setButtonsState(started)
        with(binding) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    timeFlow.collect { duration ->
                        binding.time.text = duration.toDisplayString()
                    }
                }
            }
            btnStart.setOnClickListener {
                started = true
            }
            btnStop.setOnClickListener {
                started = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(TIME, timeFlow.value.inWholeMilliseconds)
        outState.putBoolean(STARTED, started)
    }

    private fun startTimer() {
        stopTimer()
        timerJob = lifecycle.coroutineScope.launch {
            val prevTime = System.currentTimeMillis()
            while(isActive){
                val curTime = System.currentTimeMillis()
                timeFlow.emit((curTime-prevTime).milliseconds)
                delay(DELAY)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TIME = "time"
        private const val STARTED = "started"

        private const val DELAY:Long = 50

        private fun Duration.toDisplayString(): String = String.format(
            Locale.getDefault(),
            "%02d:%02d.%03d",
            this.inWholeMinutes.toInt(),
            this.inWholeSeconds.toInt(),
            this.inWholeMilliseconds.toInt()
        )
    }
}
package com.example.audio.dsp

import kotlin.math.*

enum class RiffStyle(val displayName: String) {
    ROCK_RIFF("Heavy Metal Powerchords"),
    CLEAN_CHORDS("Boutique Clean Arpeggio"),
    BLUES_LICK("Singing Blues Pentatonic")
}

class VirtualGuitarRiff(private val sampleRate: Float) {
    var isEnabled: Boolean = false
    var style: RiffStyle = RiffStyle.ROCK_RIFF

    private var sampleCounter = 0
    private var currentNoteIdx = 0
    private var phase = 0.0f
    private var noteTimer = 0

    // MIDI pitches for realistic riffs
    private val rockNotes = intArrayOf(40, 40, 43, 40, 45, 40, 43, 42) // E2, G2, A2, etc.
    private val cleanNotes = intArrayOf(52, 55, 59, 64, 59, 55, 52, 48) // E3 minor chord arpeggios
    private val bluesNotes = intArrayOf(57, 60, 62, 63, 62, 60, 57, 55) // A minor blues lick

    fun generateNext(outBuffer: FloatArray, size: Int) {
        if (!isEnabled) {
            for (i in 0 until size) outBuffer[i] = 0.0f
            return
        }

        val notes = when (style) {
            RiffStyle.ROCK_RIFF -> rockNotes
            RiffStyle.CLEAN_CHORDS -> cleanNotes
            RiffStyle.BLUES_LICK -> bluesNotes
        }

        val noteDurationSamples = (sampleRate * 0.28f).toInt()

        for (i in 0 until size) {
            noteTimer++
            if (noteTimer >= noteDurationSamples) {
                noteTimer = 0
                currentNoteIdx = (currentNoteIdx + 1) % notes.size
            }

            val midi = notes[currentNoteIdx]
            val freq = 440.0f * 2.0f.pow((midi - 69.0f) / 12.0f)

            phase += 2.0f * Math.PI.toFloat() * freq / sampleRate
            if (phase >= 2.0f * Math.PI.toFloat()) phase -= 2.0f * Math.PI.toFloat()

            // Electric guitar string synthesis: fundamental + harmonics + decay envelope
            val decay = exp(-noteTimer.toFloat() / (sampleRate * 0.25f))
            val fund = sin(phase)
            val h2 = sin(phase * 2.0f) * 0.45f
            val h3 = sin(phase * 3.0f) * 0.25f
            val h4 = sin(phase * 4.0f) * 0.15f
            val pluck = (fund + h2 + h3 + h4) * decay * 0.35f

            outBuffer[i] = pluck
        }
    }
}

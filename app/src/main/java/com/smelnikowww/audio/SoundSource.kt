package com.smelnikowww.audio

import androidx.annotation.RawRes
import com.smelnikowww.R

/**
 * Replaceable chime resource. To change the final sound, drop a new WAV into
 * `res/raw` and point this enum at it - no logic changes required.
 */
enum class SoundSource(@get:RawRes val resId: Int) {
    /** Interval and start chime. */
    BELL(R.raw.chime_soft),

    /** Longer, deeper bell played when the session completes. */
    CLOSING(R.raw.chime_end),
}

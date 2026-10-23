package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameSoundEngine {

  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val sampleRate = 22050

  // Pre-generate PCM buffers for instant zero-latency playback
  private val boneCrunchBuffer: ShortArray by lazy { generateBoneCrunchBuffer() }
  private val thudBuffer: ShortArray by lazy { generateThudBuffer() }
  private val metallicClangBuffer: ShortArray by lazy { generateMetallicClangBuffer() }
  private val explosionBuffer: ShortArray by lazy { generateExplosionBuffer() }
  private val whooshBuffer: ShortArray by lazy { generateWhooshBuffer() }

  fun playBoneCrunch() {
    playPcmBuffer(boneCrunchBuffer)
  }

  fun playThud(intensity: Float = 1.0f) {
    playPcmBuffer(thudBuffer, volume = (intensity * 0.8f).coerceIn(0.2f, 1.0f))
  }

  fun playMetallicClang() {
    playPcmBuffer(metallicClangBuffer, volume = 0.9f)
  }

  fun playExplosion() {
    playPcmBuffer(explosionBuffer, volume = 1.0f)
  }

  fun playWhoosh() {
    playPcmBuffer(whooshBuffer, volume = 0.6f)
  }

  private fun playPcmBuffer(buffer: ShortArray, volume: Float = 1.0f) {
    scope.launch {
      try {
        val audioTrack = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(buffer.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        audioTrack.setVolume(volume.coerceIn(0f, 1f))
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()

        // Clean up when done
        val durationMs = (buffer.size * 1000L) / sampleRate + 50L
        kotlinx.coroutines.delay(durationMs)
        audioTrack.stop()
        audioTrack.release()
      } catch (_: Exception) {
        // Safe fallback if audio hardware is busy
      }
    }
  }

  private fun generateBoneCrunchBuffer(): ShortArray {
    // Sharp snap + high-frequency noise bursts (fracturing bone crunch)
    val durationSamples = (sampleRate * 0.16f).toInt()
    val buffer = ShortArray(durationSamples)

    for (i in 0 until durationSamples) {
      val t = i.toFloat() / sampleRate
      val env = exp(-t * 28f) // Sharp decay

      // Random crackle pulses + high mid frequencies
      val noise = (Random.nextFloat() * 2f - 1f)
      val snap = sin(2f * PI * 180f * t) * exp(-t * 45f)
      val crunch = sin(2f * PI * 850f * t + noise * 1.5f)

      val sample = (noise * 0.5f + snap * 0.35f + crunch * 0.45f) * env
      buffer[i] = (sample.toFloat().coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.85f).toInt().toShort()
    }
    return buffer
  }

  private fun generateThudBuffer(): ShortArray {
    // Low frequency dull punch for concrete impact
    val durationSamples = (sampleRate * 0.18f).toInt()
    val buffer = ShortArray(durationSamples)

    for (i in 0 until durationSamples) {
      val t = i.toFloat() / sampleRate
      val env = exp(-t * 18f)

      // Frequency drops rapidly from 130Hz to 45Hz
      val freq = 130f - t * 450f
      val wave = sin(2f * PI * freq.coerceAtLeast(35f) * t)

      val sample = wave * env
      buffer[i] = (sample.toFloat().coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.8f).toInt().toShort()
    }
    return buffer
  }

  private fun generateMetallicClangBuffer(): ShortArray {
    // Resonant metallic harmonic cluster
    val durationSamples = (sampleRate * 0.28f).toInt()
    val buffer = ShortArray(durationSamples)

    for (i in 0 until durationSamples) {
      val t = i.toFloat() / sampleRate
      val env = exp(-t * 12f)

      // Inharmonic frequencies typical of metal sheets / vehicle chassis
      val f1 = sin(2f * PI * 440f * t)
      val f2 = sin(2f * PI * 720f * t) * 0.7f
      val f3 = sin(2f * PI * 1150f * t) * 0.5f
      val f4 = sin(2f * PI * 1820f * t) * 0.3f

      val sample = (f1 + f2 + f3 + f4) * 0.4f * env
      buffer[i] = (sample.toFloat().coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.85f).toInt().toShort()
    }
    return buffer
  }

  private fun generateExplosionBuffer(): ShortArray {
    // Heavy low-pass noise rumble
    val durationSamples = (sampleRate * 0.45f).toInt()
    val buffer = ShortArray(durationSamples)
    var lastNoise = 0f

    for (i in 0 until durationSamples) {
      val t = i.toFloat() / sampleRate
      val env = exp(-t * 6f)

      val rawNoise = Random.nextFloat() * 2f - 1f
      // Simple low pass filter
      lastNoise = lastNoise * 0.85f + rawNoise * 0.15f

      val sub = sin(2f * PI * 55f * t) * 0.4f
      val sample = (lastNoise * 0.7f + sub) * env
      buffer[i] = (sample.toFloat().coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.95f).toInt().toShort()
    }
    return buffer
  }

  private fun generateWhooshBuffer(): ShortArray {
    // Fast airy pitch-bent sweep
    val durationSamples = (sampleRate * 0.15f).toInt()
    val buffer = ShortArray(durationSamples)

    for (i in 0 until durationSamples) {
      val t = i.toFloat() / sampleRate
      val env = sin(PI.toFloat() * (i.toFloat() / durationSamples))

      val noise = Random.nextFloat() * 2f - 1f
      val sweep = sin(2f * PI * (300f + t * 600f) * t) * 0.5f

      val sample = (noise * 0.6f + sweep) * env
      buffer[i] = (sample.toFloat().coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.6f).toInt().toShort()
    }
    return buffer
  }
}

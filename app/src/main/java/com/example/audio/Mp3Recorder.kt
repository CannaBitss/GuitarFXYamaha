package com.example.audio

import android.content.Context
import android.os.Environment
import android.util.Log
import net.qiujuer.lame.Lame
import net.qiujuer.lame.LameOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Encodes already-processed output PCM to MP3 on a worker thread.
 * The real-time audio thread only copies into a preallocated lock-free-in-practice pool
 * and never performs file I/O or MP3 encoding.
 */
class Mp3Recorder(
    context: Context,
    private val inputSampleRate: Int,
    private val framesPerBuffer: Int = 512,
    private val onFinished: (File) -> Unit = {},
    private val onError: (Throwable) -> Unit = {}
) {
    private class Chunk(val samples: ShortArray, @Volatile var size: Int = 0)

    private val queue = ArrayBlockingQueue<Chunk>(24)
    private val pool = ArrayBlockingQueue<Chunk>(24)
    private val stopChunk = Chunk(ShortArray(0), -1)
    private val droppedChunks = AtomicInteger(0)
    @Volatile private var accepting = false
    private var worker: Thread? = null
    private val outputDirectory: File = File(
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir,
        "GuitarFXYamaha"
    )

    @Volatile
    var outputFile: File? = null
        private set

    fun start(): File {
        if (accepting) return outputFile ?: error("Recorder is starting")
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            error("Cannot create recording directory: ${outputDirectory.absolutePath}")
        }

        val file = File(outputDirectory, "GuitarFX_${System.currentTimeMillis()}.mp3")
        repeat(24) { pool.offer(Chunk(ShortArray(framesPerBuffer))) }
        queue.clear()
        droppedChunks.set(0)
        outputFile = file
        accepting = true

        worker = Thread({
            var encoder: LameOutputStream? = null
            try {
                val stream = BufferedOutputStream(FileOutputStream(file), 16 * 1024)
                val lame = Lame(
                    inputSampleRate,
                    1,
                    44100,
                    192,
                    Lame.LameModel.MONO,
                    Lame.LameQuality.GOOD
                )
                encoder = LameOutputStream(lame, stream, framesPerBuffer)

                while (true) {
                    val chunk = queue.take()
                    if (chunk === stopChunk) break
                    if (chunk.size > 0) encoder.write(chunk.samples, chunk.size)
                    chunk.size = 0
                    pool.offer(chunk)
                }
                encoder.close()
                encoder = null
                Log.i("Mp3Recorder", "Saved MP3: ${file.absolutePath}; dropped chunks=${droppedChunks.get()}")
                onFinished(file)
            } catch (t: Throwable) {
                accepting = false
                try { encoder?.close() } catch (_: Throwable) {}
                Log.e("Mp3Recorder", "MP3 encoding failed", t)
                onError(t)
            }
        }, "GuitarFxMp3Encoder").apply {
            priority = Thread.NORM_PRIORITY
            start()
        }
        return file
    }

    /** Called from the audio thread. Never blocks and never allocates. */
    fun write(samples: ShortArray, size: Int) {
        if (!accepting || size <= 0) return
        val chunk = pool.poll()
        if (chunk == null) {
            droppedChunks.incrementAndGet()
            return
        }
        val count = size.coerceAtMost(chunk.samples.size)
        System.arraycopy(samples, 0, chunk.samples, 0, count)
        chunk.size = count
        if (!queue.offer(chunk)) {
            chunk.size = 0
            pool.offer(chunk)
            droppedChunks.incrementAndGet()
        }
    }

    fun stop() {
        if (!accepting) return
        accepting = false
        // Enqueue after all pending PCM. If the bounded queue is momentarily full,
        // a tiny control thread waits rather than blocking the UI or audio callback.
        if (!queue.offer(stopChunk)) {
            Thread({
                try {
                    queue.put(stopChunk)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }, "GuitarFxMp3Stop").start()
        }
    }
}

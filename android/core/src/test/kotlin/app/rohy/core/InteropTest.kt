package app.rohy.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.BufferedReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * End-to-end test of the Kotlin client against the real Swift server (mac/Sources/rohy-harness).
 * Runs when ROHY_HARNESS points to the built harness (scripts/check-android.sh does this on macOS).
 */
class InteropTest {
    private class Recorder : RohyClient.Listener {
        val paired = LinkedBlockingQueue<String>()
        val ready = CountDownLatch(1)
        val clips = LinkedBlockingQueue<Clip>()
        val closed = LinkedBlockingQueue<String>()

        override fun onPaired(macName: String) {
            paired.add(macName)
        }

        override fun onReady() = ready.countDown()

        override fun onClip(clip: Clip) {
            clips.add(clip)
        }

        override fun onClosed(code: Int?, reason: String) {
            closed.add("$code $reason")
        }
    }

    private fun BufferedReader.expect(prefix: String): String {
        while (true) {
            val line = readLine() ?: error("harness exited while waiting for $prefix")
            if (line.startsWith(prefix)) return line.removePrefix(prefix)
        }
    }

    @Test
    fun pairsSendsReceivesAndReconnectsAgainstSwiftServer() {
        val harness = System.getenv("ROHY_HARNESS")
        assumeTrue("ROHY_HARNESS not set", !harness.isNullOrEmpty())
        val process = ProcessBuilder(harness).redirectErrorStream(true).start()
        try {
            val output = process.inputStream.bufferedReader()
            val input = process.outputStream.bufferedWriter()
            val payload = requireNotNull(PairingPayload.parse(output.expect("READY ")))
            val phone = Identity.generate()

            val first = Recorder()
            val client = RohyClient(ClientSession(phone, payload.publicKey, "Kotlin test", payload.token), first)
            client.send(Clip(text = "envoyé avant l'appairage"))
            client.connect(payload.hosts.first(), payload.port)
            assertEquals("Rohy harness", first.paired.poll(10, TimeUnit.SECONDS))
            assertEquals(true, first.ready.await(5, TimeUnit.SECONDS))
            assertEquals("Kotlin test", output.expect("PAIRED "))
            assertEquals("envoyé avant l'appairage", output.expect("RECEIVED "))

            client.send(Clip(text = "Code wifi : rose-mango-42 éà"))
            assertEquals("Code wifi : rose-mango-42 éà", output.expect("RECEIVED "))

            input.write("SEND depuis le Mac\n")
            input.flush()
            assertEquals("depuis le Mac", first.clips.poll(10, TimeUnit.SECONDS)?.text)
            client.close()
            assertNotNull(first.closed.poll(10, TimeUnit.SECONDS))
            assertEquals(emptyList<Clip>(), client.drainPending())

            // Second connection: already trusted, no token needed.
            val second = Recorder()
            val again = RohyClient(ClientSession(phone, payload.publicKey, "Kotlin test"), second)
            again.connect(payload.hosts.first(), payload.port)
            assertEquals(true, second.ready.await(10, TimeUnit.SECONDS))
            again.send(Clip(text = "reconnecté"))
            assertEquals("reconnecté", output.expect("RECEIVED "))
            again.close()

            // A stranger without token is rejected.
            val stranger = Recorder()
            RohyClient(ClientSession(Identity.generate(), payload.publicKey, "Intrus"), stranger)
                .connect(payload.hosts.first(), payload.port)
            assertEquals("4403", stranger.closed.poll(10, TimeUnit.SECONDS)?.substringBefore(' '))
            assertEquals("never ready", 1L, stranger.ready.count)
        } finally {
            process.destroy()
        }
    }
}

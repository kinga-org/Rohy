package app.rohy

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.InetAddress
import app.rohy.core.RohyProtocol
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Finds the paired Mac on the local network through Bonjour (NSD), matching the key fingerprint. */
class MacLocator(private val context: Context) {
    /** Blocks up to [timeoutMillis]; returns the Mac's address or null. Call off the main thread. */
    @Suppress("DEPRECATION")
    fun find(fingerprint: String, timeoutMillis: Long = 3_000): String? {
        val nsd = context.getSystemService(NsdManager::class.java) ?: return null
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        val lock = wifi?.createMulticastLock("rohy")?.apply {
            setReferenceCounted(false)
            acquire()
        }
        val found = AtomicReference<String?>()
        val latch = CountDownLatch(1)

        val resolver = object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit

            override fun onServiceResolved(info: NsdServiceInfo) {
                val fp = info.attributes["fp"]?.decodeToString()
                val host = preferredAddress(info)
                if (fp == fingerprint && host != null) {
                    found.set(host)
                    latch.countDown()
                }
            }
        }
        val discovery = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = latch.countDown()
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceLost(info: NsdServiceInfo) = Unit
            override fun onServiceFound(info: NsdServiceInfo) {
                runCatching { nsd.resolveService(info, resolver) }
            }
        }

        try {
            nsd.discoverServices(RohyProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discovery)
            latch.await(timeoutMillis, TimeUnit.MILLISECONDS)
        } finally {
            runCatching { nsd.stopServiceDiscovery(discovery) }
            lock?.release()
        }
        return found.get()
    }

    /** IPv4 first: link-local IPv6 addresses carry a scope that does not fit in a URL. */
    @Suppress("DEPRECATION")
    private fun preferredAddress(info: NsdServiceInfo): String? {
        val addresses: List<InetAddress> =
            if (Build.VERSION.SDK_INT >= 34) info.hostAddresses else listOfNotNull(info.host)
        return addresses.firstOrNull { it is Inet4Address }?.hostAddress
    }
}

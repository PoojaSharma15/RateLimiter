import java.util.*
import kotlin.collections.HashMap

class RequestTimestamps(private val requests: Int, private val windowTimeInSec: Int) {
    private val timestamps = ArrayDeque<Long>()
    private val lock = Object()

    private fun evictOlderTimestamps(currentTimestamp: Long) {
        synchronized(lock) {
            while (timestamps.isNotEmpty() && currentTimestamp - timestamps.first() > windowTimeInSec) {
                timestamps.removeFirst()
            }
        }
    }

    fun addTimestamp(timestamp: Long) {
        synchronized(lock) {
            timestamps.addLast(timestamp)
        }
    }

    fun shouldAllowRequest(currentTimestamp: Long): Boolean {
        synchronized(lock) {
            evictOlderTimestamps(currentTimestamp)
            return timestamps.size <= requests
        }
    }
}

class SlidingWindowLogsRateLimiter {
    //rateLimiterMap here corresponds to redis cluster created per userId
    private val ratelimiterMap = HashMap<Int, RequestTimestamps>()
    private val lock = Object()

    fun addUser(userId: Int, requests: Int = 100, windowTimeInSec: Int = 60) {
        synchronized(lock) {
            if (ratelimiterMap.containsKey(userId)) {
                throw IllegalArgumentException("User already present")
            }
            ratelimiterMap[userId] = RequestTimestamps(requests, windowTimeInSec)
        }
    }

    fun removeUser(userId: Int) {
        synchronized(lock) {
            ratelimiterMap.remove(userId)
        }
    }

    fun getCurrentTimestampInSec(): Long {
        return System.currentTimeMillis() / 1000
    }

    fun shouldAllowServiceCall(userId: Int): Boolean {
        synchronized(lock) {
            val userTimestamps = ratelimiterMap[userId] ?: throw IllegalArgumentException("User is not present. Please whitelist and register the user for service")
            val currentTimestamp = getCurrentTimestampInSec()
            if (!userTimestamps.shouldAllowRequest(currentTimestamp)) {
                return false
            }
            userTimestamps.addTimestamp(currentTimestamp)
            return true
        }
    }
}

fun main() {
    val limiter = SlidingWindowLogsRateLimiter()
    limiter.addUser(1)

    // Simulate service calls
    for (i in 0..<200) {
        if (limiter.shouldAllowServiceCall(1)) {
            println("Service call allowed")
        } else {
            println("Service call rejected")
        }
        Thread.sleep(100)
    }
}

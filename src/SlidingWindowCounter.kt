/*
A rate limiter of 100 req/min
WindowTimeInSec = 60
No of Buckets = 10
Each Bucket length = 6

*/

class RequestCounters(private val requestLimit: Int, private val windowTimeInSec: Int, private val bucketSize: Int = 10){
    private val lock = Object()
    private var totalCounts = 0
    private val counts = HashMap<Long, Int>()

    fun evictOlderBuckets(currentTimestamp: Long) {
        synchronized(lock) {
            val oldestValidBucket = getBucket(currentTimestamp - windowTimeInSec)
            val bucketsToBeDeleted = counts.keys.filter { it < oldestValidBucket }
            bucketsToBeDeleted.forEach { bucket ->
                val bucketCount = counts[bucket] ?: 0
                totalCounts -= bucketCount
                counts.remove(bucket)
            }
        }
    }

    private fun getBucket(timestamp: Long): Long{
        val factor = windowTimeInSec / bucketSize
        val timeIndex = timestamp / windowTimeInSec
        return timeIndex * factor

    }

    fun incrementBucketCount(currentTimeStamp: Long){
        synchronized(lock) {
            val currentBucket = getBucket(currentTimeStamp)
            counts[currentBucket] = (counts[currentBucket] ?: 0) + 1
            totalCounts++
        }
    }

    fun isWithinRateLimit(): Boolean {
        return totalCounts < requestLimit
    }
}

class SlidingWindowCounterLimiter {

    //rateLimiterMap here corresponds to redis cluster created per userId
    private val rateLimiterMap = HashMap<Int, RequestCounters>()
    private val lock = Object()

    //Default params for 100 requests/min
    fun addUser(userid : Int, requests: Int = 100, windowTimeInSec: Int = 60){
        synchronized(lock) {
            if (rateLimiterMap.containsKey(userid)) {
                throw IllegalArgumentException("UserId Already present")
            }
            rateLimiterMap[userid] = RequestCounters(requests, windowTimeInSec)
        }
    }

    fun removeUser(userid: Int){
       synchronized(lock){
           rateLimiterMap.remove(userid)
       }
    }

    private fun getCurrentTimestampInSec(): Long {
        return System.currentTimeMillis() / 1000
    }

    fun shouldAllowServiceCall(userid: Int): Boolean{
        val requestCounter = rateLimiterMap[userid] ?: throw IllegalArgumentException("UserId not whitelisted")
        val currentTimestamp = getCurrentTimestampInSec()
        requestCounter.evictOlderBuckets(currentTimestamp)
        requestCounter.incrementBucketCount(currentTimestamp)
        return requestCounter.isWithinRateLimit()

    }
}

fun main(){

    val limiter = SlidingWindowCounterLimiter()
    limiter.addUser(1)

    // Simulate service calls
    for (i in 1..<150) {
        if (limiter.shouldAllowServiceCall(1)) {
            println("Service call number $i allowed")
        } else {
            println("Service call number $i rejected")
        }
        Thread.sleep(100)
    }
}
package janbrodhaecker.de.bitcointracker.remote.`interface`

import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by jan.brodhaecker on 09.09.18.
 */
class BitcoinAverageHistoricalData {

    private var average: String = ""
    private var timestamp: String = ""

    constructor(jsonObject: JSONObject) {
        average = jsonObject.getString("average")
        timestamp = jsonObject.getString("time")
    }

    fun getAverage(): Float {
        return average.toFloat()
    }

    fun getTimestamp(): Float {
        val pattern = "yyyy-MM-dd HH:mm:ss";
        val formatter = SimpleDateFormat(pattern)
        return formatter.parse(timestamp).time.toFloat()
    }

}
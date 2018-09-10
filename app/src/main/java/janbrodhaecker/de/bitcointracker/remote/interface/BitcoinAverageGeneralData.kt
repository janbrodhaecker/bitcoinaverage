package janbrodhaecker.de.bitcointracker.remote.`interface`

import org.json.JSONObject

/**
 * Created by jan.brodhaecker on 09.09.18.
 */
class BitcoinAverageGeneralData {

    private var timeStamp: Long = 0
    private var currentValue: Double = 0.0
    private var percentDay: Double = 0.0
    private var priceDay: Double = 0.0
    private var todaysOpen: Double = 0.0
    private var todaysHigh: Double = 0.0
    private var todaysLow: Double = 0.0
    private var average24h: Double = 0.0
    private var globalVolume: Double = 0.0

    constructor(jsonObject: JSONObject) {
        // We definitely should improve the error handling here
        timeStamp = jsonObject.getLong("timestamp")
        currentValue = jsonObject.getDouble("ask")
        percentDay = jsonObject.getJSONObject("changes")
                .getJSONObject("percent")
                .getDouble("day")
        priceDay = jsonObject.getJSONObject("changes")
                .getJSONObject("price")
                .getDouble("day")
        todaysOpen = jsonObject.getJSONObject("open").getDouble("day")
        todaysHigh = jsonObject.getDouble("high")
        todaysLow = jsonObject.getDouble("low")
        average24h = jsonObject.getJSONObject("averages").getDouble("day")
        globalVolume = jsonObject.getDouble("volume")
    }

    fun getTimeStamp(): Long {
        return timeStamp
    }

    fun getCurrentValue(): Double {
        return currentValue
    }

    fun getPercentDay(): Double {
        return percentDay
    }

    fun getPriceDay(): Double {
        return priceDay
    }

    fun getTodaysOpen(): Double {
        return todaysOpen
    }

    fun getTodaysHigh(): Double {
        return todaysHigh
    }

    fun getTodaysLow(): Double {
        return todaysLow
    }

    fun getAverage24h(): Double {
        return average24h
    }

    fun getGlobalVolume(): Double {
        return globalVolume
    }

}
package janbrodhaecker.de.bitcointracker.remote.`interface`

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.*
import okio.ByteString
import org.apache.commons.codec.binary.Hex
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by jan.brodhaecker on 08.09.18.
 */

class BitcoinAverageRemoteInterface {

    enum class ChartPeriod {
        daily,
        monthly,
        alltime
    }

    companion object {
        val PUBLIC_KEY: String = ""
        val PRIVATE_KEY: String = ""

        fun generateWebSocketURL(ticket: String): String {
            return "wss://apiv2.bitcoinaverage.com/websocket/ticker?ticket=$ticket&public_key=$PUBLIC_KEY"
        }

        fun getHistoricalDataURL(currencySymbol: String, period: ChartPeriod): String {
            return "https://apiv2.bitcoinaverage.com/indices/global/history/$currencySymbol?period=${period.name}&format=json"
        }
    }


    private val client: OkHttpClient = OkHttpClient()
    private val generalDataSubject: PublishSubject<BitcoinAverageGeneralData> = PublishSubject.create()
    private val stateSubject: PublishSubject<RemoteState> = PublishSubject.create()
    private lateinit var webSocket: WebSocket

    private lateinit var availableCurrencies: List<BitcoinAverageCurrency>
    private lateinit var currentCurrency: BitcoinAverageCurrency

    init {
        stateSubject.onNext(RemoteState.UNITIALIZED)

        val usdCurrency = BitcoinAverageCurrency("BTCUSD", "USD", "$")
        val eurCurrency = BitcoinAverageCurrency("BTCEUR", "EUR", "€")

        currentCurrency = usdCurrency
        availableCurrencies = listOf(usdCurrency, eurCurrency)
    }

    fun initialize(): Observable<Boolean> {
        return getTicket().flatMap({ticket ->
                    createWebSocket(ticket)
            })
    }

    fun getRemoteState(): Observable<RemoteState> {
       return stateSubject
    }

    fun subscribeToGeneralData(): Observable<BitcoinAverageGeneralData> {
        return generalDataSubject;
    }

    fun terminate(): Boolean {
        if (webSocket != null) {
            return webSocket.close(1000, "Closing Websocket on request.")
        }
        return false
    }

    fun getAvailableCurrencies(): List<BitcoinAverageCurrency> {
        return availableCurrencies
    }

    fun setCurrency(bitcoinCurrency: BitcoinAverageCurrency) {
        currentCurrency = bitcoinCurrency
    }

    fun getcurrentCurrency(): BitcoinAverageCurrency {
        return currentCurrency
    }

    fun getHistoricalData(chartPeriod: ChartPeriod): Observable<List<BitcoinAverageHistoricalData>> {
        return Observable.create { subscriber ->
            var request = Request.Builder()
                    .url(BitcoinAverageRemoteInterface.getHistoricalDataURL(currentCurrency.getKey(), chartPeriod))
                    .build()

            client.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    subscriber.onError(Error(e.toString()))
                }

                override fun onResponse(call: Call?, response: Response?) {
                    val jsonResponse: JSONArray = JSONArray(response?.body()?.string())

                    var result = ArrayList<BitcoinAverageHistoricalData>()
                    for (cnt  in 0 .. jsonResponse.length() - 1) {
                        var jsonObject: JSONObject = jsonResponse.get(cnt) as JSONObject
                        result.add(BitcoinAverageHistoricalData(jsonObject))
                    }
                    subscriber.onNext(result)
                    subscriber.onComplete()
                }
            })
        }
    }

    private fun getTicket(): Observable<String> {
        return Observable.create { subscriber ->
            val request = Request.Builder()
                    .addHeader("X-signature", generateSignature())
                    .url("https://apiv2.bitcoinaverage.com/websocket/get_ticket")
                    .build()

            client.newCall(request).enqueue(object: Callback {

                override fun onFailure(call: Call?, e: IOException?) {
                    subscriber.onError(Error(e.toString()))
                    stateSubject.onNext(RemoteState.ERROR)
                }

                override fun onResponse(call: Call?, response: Response?) {
                    val jsonResponse : JSONObject = JSONObject(response?.body()?.string())
                    val ticket : String = jsonResponse.getString("ticket")
                    subscriber.onNext(ticket)
                    subscriber.onComplete()
                }
            })
        }
    }


    private fun generateSignature() : String {
        val payloadPrefix = getUnixEpoch().toString() + "." + BitcoinAverageRemoteInterface.PUBLIC_KEY
        return payloadPrefix + "." + generateDigestValue(payloadPrefix, BitcoinAverageRemoteInterface.PRIVATE_KEY)
    }

    private fun getUnixEpoch() : Long {
        return System.currentTimeMillis() / 1000L
    }

    private fun generateDigestValue(payload: String, privateKey: String): String {
        val sha256_HMAC = Mac.getInstance("HmacSHA256")
        val secret_key = SecretKeySpec(BitcoinAverageRemoteInterface.PRIVATE_KEY.toByteArray(), "HmacSHA256")
        sha256_HMAC.init(secret_key)

        val encodedString = String(Hex.encodeHex(sha256_HMAC.doFinal(payload.toByteArray())))
        return encodedString
    }

    private fun createWebSocket(ticket: String): Observable<Boolean> {
        return Observable.create { subscriber ->
            val request: Request = Request.Builder().url(BitcoinAverageRemoteInterface.generateWebSocketURL(ticket)).build()
            val client = OkHttpClient()

            webSocket = client.newWebSocket(request, object: WebSocketListener() {

                override fun onOpen(webSocket: WebSocket?, response: Response?) {
                    super.onOpen(webSocket, response)
                    stateSubject.onNext(RemoteState.CONNECTING)
                }

                override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
                    super.onClosed(webSocket, code, reason)
                    stateSubject.onNext(RemoteState.DISCONNECTED)
                }

                override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
                    super.onClosing(webSocket, code, reason)
                    stateSubject.onNext(RemoteState.DISCONNECTING)
                }

                override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
                    super.onFailure(webSocket, t, response)
                    stateSubject.onNext(RemoteState.ERROR)
                    subscriber.onNext(false)
                }

                override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
                    super.onMessage(webSocket, bytes)
                }

                override fun onMessage(webSocket: WebSocket?, text: String?) {
                    webSocket?.send(subscribeMessage())

                    val jsonResponse: JSONObject = JSONObject(text)
                    if (jsonResponse.getString("data") == "OK" && jsonResponse.getString("type") == "status") {
                        stateSubject.onNext(RemoteState.CONNECTED)
                        subscriber.onNext(false)
                    } else if (jsonResponse.getString("event") == "message") {
                        val data = BitcoinAverageGeneralData(jsonResponse.getJSONObject("data"))
                        generalDataSubject.onNext(data)
                    }
                }
            })
        }
    }

    private fun subscribeMessage(): String {
        val optionsJSONObject: JSONObject = JSONObject()
                .put("currency", currentCurrency.getKey())
                .put("symbol_set", "global")

        val dataJSONObject: JSONObject = JSONObject()
                .put("operation", "subscribe")
                .put("options", optionsJSONObject)

        val jsonObject: JSONObject = JSONObject()
                .put("event", "message")
                .put("data", dataJSONObject)

        return jsonObject.toString()
    }
}

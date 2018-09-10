package janbrodhaecker.de.bitcointracker.remote.`interface`

/**
 * Created by jan.brodhaecker on 09.09.18.
 */
class BitcoinAverageCurrency {

    private var key: String
    private var text: String
    private var symbol: String

    constructor(_key: String, _text: String, _symbol: String) {
        key = _key
        text = _text
        symbol = _symbol
    }

    fun getKey(): String {
        return key
    }

    fun getText(): String {
        return text
    }

    fun getSymbol(): String {
        return symbol
    }

}
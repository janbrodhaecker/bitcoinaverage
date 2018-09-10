package janbrodhaecker.de.bitcointracker.remote.`interface`

/**
 * Created by jan.brodhaecker on 08.09.18.
 */
enum class RemoteState {

    UNITIALIZED,
    CONNECTING,
    CONNECTED,
    ERROR,
    DISCONNECTING,
    DISCONNECTED
}
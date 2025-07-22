package space.livedigital.example.logger

import android.util.Log
import space.livedigital.sdk.debug.logger.Level
import space.livedigital.sdk.debug.logger.Logger

object ConsoleLogger : Logger {
    private const val LOG_PREFIX = "LD_SDK_"

    override fun log(level: Level, tag: String, message: String, payload: Map<String, String?>?) {
        val consoleLogTag = LOG_PREFIX + tag
        var consoleMessage = message
        if (payload.isNullOrEmpty().not()) {
            consoleMessage += ": " + payload.toString().trimStart('{').trimEnd('}')
        }

        when (level) {
            Level.VERBOSE -> Log.v(consoleLogTag, consoleMessage)
            Level.DEBUG -> Log.d(consoleLogTag, consoleMessage)
            Level.INFO -> Log.i(consoleLogTag, consoleMessage)
            Level.WARN -> Log.w(consoleLogTag, consoleMessage)
            Level.ERROR -> Log.e(consoleLogTag, consoleMessage)
        }
    }
}
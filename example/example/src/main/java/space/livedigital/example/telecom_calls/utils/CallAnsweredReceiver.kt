package space.livedigital.example.telecom_calls.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import space.livedigital.example.SessionActivity

class CallAnsweredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        if (intent?.action == ACTION_CALL_ANSWERED) {
            val roomAlias = intent.getStringExtra(ROOM_ALIAS_KEY) ?: return
            val activityIntent = Intent(context, SessionActivity::class.java).apply {
                putExtra(ROOM_ALIAS_KEY, roomAlias)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(activityIntent)
        }
    }

    companion object {
        const val ACTION_CALL_ANSWERED = "ACTION_CALL_ANSWERED"
        const val ROOM_ALIAS_KEY =  "ROOM_ALIAS_KEY"
    }
}
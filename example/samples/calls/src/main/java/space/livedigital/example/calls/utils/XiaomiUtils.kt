package space.livedigital.example.calls.utils

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.text.TextUtils
import java.lang.reflect.Method


object XiaomiUtilities {
    const val OP_BACKGROUND_START_ACTIVITY: Int = 10021
    const val OP_SHOW_WHEN_LOCKED: Int = 10020

    val isMIUI: Boolean
        get() = !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"))

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String?): String? {
        try {
            val props = Class.forName("android.os.SystemProperties")
            return props.getMethod("get", String::class.java).invoke(null, key) as String?
        } catch (ignore: Exception) {
        }
        return null
    }


    fun isCustomPermissionGranted(context: Context, permission: Int): Boolean {
        try {
            val mgr = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager?
            val m: Method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result = m.invoke(mgr, permission, Process.myUid(), context.getPackageName()) as Int
            return result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {

        }
        return true
    }

    fun getPermissionManagerIntent(context: Context): Intent {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        intent.putExtra("extra_package_uid", Process.myUid())
        intent.putExtra("extra_pkgname", context.getPackageName())
        intent.putExtra("extra_package_name", context.getPackageName())
        return intent
    }
}
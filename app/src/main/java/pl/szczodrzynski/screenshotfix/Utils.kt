package pl.szczodrzynski.screenshotfix

import android.app.ActivityManager
import android.app.AndroidAppHelper
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import de.robv.android.xposed.XposedHelpers

object Utils {

    const val TAKE_SCREENSHOT_FULLSCREEN = 1
    const val TAKE_SCREENSHOT_SELECTED_REGION = 2

    val appNameRegex by lazy {
        "[\\\\/:*?\"<>|\\s]+".toRegex()
    }

    fun getScreenshotFormat(classLoader: ClassLoader): Int? {
        val screenshotClass = XposedHelpers.findClass("com.asus.stitchimage.j.g", classLoader)
        return XposedHelpers.callStaticMethod(
            screenshotClass,
            "b",
            arrayOf(ContentResolver::class.java),
            AndroidAppHelper.currentApplication().contentResolver
        ) as? Int
    }

    /* https://github.com/ResurrectionRemix/android_frameworks_base/blob/Q/packages/SystemUI/src/com/android/systemui/screenshot/GlobalScreenshot.java#L183 */
    fun getRunningActivityName(context: Context): CharSequence? {
        val am = context.getSystemService(ActivityManager::class.java)
        val pm = context.packageManager
        val tasks = am.getRunningTasks(1)
        if (tasks?.isNotEmpty() == true) {
            val top = tasks[0]
            try {
                val info = pm.getActivityInfo(top.topActivity!!, 0)
                return pm.getApplicationLabel(info.applicationInfo)
            } catch (e: PackageManager.NameNotFoundException) {

            }
        }
        return null
    }
}

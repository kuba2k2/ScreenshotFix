package pl.szczodrzynski.screenshotfix

import android.app.ActivityManager
import android.app.AndroidAppHelper
import android.app.KeyguardManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import de.robv.android.xposed.XposedHelpers

object Utils {

    const val TAKE_SCREENSHOT_FULLSCREEN = 1
    const val TAKE_SCREENSHOT_SELECTED_REGION = 2

    private val appNameRegex by lazy {
        "[\\\\/:*?\"<>|\\s]+".toRegex()
    }

    fun getAsusScreenshotFormat(classLoader: ClassLoader): Int? {
        val screenshotClass = XposedHelpers.findClass("com.asus.stitchimage.j.g", classLoader)
        return XposedHelpers.callStaticMethod(
            screenshotClass,
            "b",
            arrayOf(ContentResolver::class.java),
            AndroidAppHelper.currentApplication().contentResolver
        ) as? Int
    }

    /* https://github.com/ResurrectionRemix/android_frameworks_base/blob/Q/packages/SystemUI/src/com/android/systemui/screenshot/GlobalScreenshot.java#L183 */
    private fun getRunningActivityName(context: Context): CharSequence? {
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

    /* https://github.com/ResurrectionRemix/android_frameworks_base/blob/Q/packages/SystemUI/src/com/android/systemui/screenshot/GlobalScreenshot.java#L207 */
    fun getScreenshotFilename(context: Context, imageDate: String, extension: String): String {
        val appName = getRunningActivityName(context)
        val onKeyguard =
            context.getSystemService(KeyguardManager::class.java).isKeyguardLocked

        return if (!onKeyguard && appName != null) {
            val appNameString = appName.toString().replace(appNameRegex, "_")
            String.format(
                "Screenshot_%s_%s.%s",
                imageDate,
                appNameString,
                extension
            )
        } else {
            String.format(
                "Screenshot_%s.%s",
                imageDate,
                extension
            )
        }
    }
}

package pl.szczodrzynski.screenshotfix

import android.app.AndroidAppHelper
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Bitmap
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookConstructor
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage
import pl.szczodrzynski.screenshotfix.Utils.appNameRegex
import pl.szczodrzynski.screenshotfix.Utils.getRunningActivityName
import pl.szczodrzynski.screenshotfix.Utils.getScreenshotFormat
import java.io.File
import java.io.OutputStream
import java.util.*

class StitchImageHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam?.packageName != "com.asus.stitchimage")
            return

        /* hook the Bitmap.compress() method to make screenshot editor respect the format */
        findAndHookMethod(
            "android.graphics.Bitmap",
            lpparam.classLoader,
            "compress",
            /* format */ Bitmap.CompressFormat::class.java,
            /* quality */ Integer.TYPE,
            /* stream */ OutputStream::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val screenshotFormat = getScreenshotFormat(lpparam.classLoader)

                    param.args[0] =
                        if (screenshotFormat == 0) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
                }
            })

        /* hook the SimpleDateFormat constructor to change the screenshot filename format */
        findAndHookConstructor(
            "java.text.SimpleDateFormat",
            lpparam.classLoader,
            String::class.java, Locale::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    when (param.args[0]) {
                        "yyyyMMdd-HHmmssSSS",
                        "yyyyMMdd-HHmmss" -> {
                            param.args[0] = "yyyy-MM-dd_HH-mm-ss"
                        }
                    }
                }
            })

        /* hook the screenshot editor to replace files instead of adding "file (1)" copies */
        findAndHookMethod(
            "com.asus.stitchimage.editor.s",
            lpparam.classLoader,
            "a",
            File::class.java, Context::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = param.args[0] as? File?
                    if (file != null) {
                        // return the same file with original name
                        param.result = file
                    }
                    // calls the original method if file == null
                }
            })

        /* hook the String.format() method to append the current app name to filename */
        findAndHookMethod(
            "java.lang.String",
            lpparam.classLoader,
            "format",
            Locale::class.java, String::class.java, emptyArray<Any>()::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val format = param.args[1] as String
                    if (format != "Screenshot_%s.%s")
                        return
                    val locale = param.args[0] as Locale
                    val args = param.args[2] as Array<*>

                    val context = AndroidAppHelper.currentApplication().baseContext

                    /* https://github.com/ResurrectionRemix/android_frameworks_base/blob/Q/packages/SystemUI/src/com/android/systemui/screenshot/GlobalScreenshot.java#L207 */
                    val imageDate = args[0] as String
                    val extension = args[1] as String

                    val appName = getRunningActivityName(context)
                    val onKeyguard =
                        context.getSystemService(KeyguardManager::class.java).isKeyguardLocked

                    if (!onKeyguard && appName != null) {
                        val appNameString = appName.toString().replace(appNameRegex, "_")
                        param.result = String.format(
                            locale,
                            "Screenshot_%s_%s.%s",
                            imageDate,
                            appNameString,
                            extension
                        )
                    } else {
                        param.result = String.format(
                            locale,
                            format,
                            imageDate,
                            extension
                        )
                    }
                }
            })
    }
}

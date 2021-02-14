package pl.szczodrzynski.screenshotfix

import android.app.AndroidAppHelper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import pl.szczodrzynski.screenshotfix.Utils.getScreenshotFilename
import java.text.SimpleDateFormat
import java.util.*

class SystemUIHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam?.packageName != "com.android.systemui")
            return

        /* hook SystemUI GlobalScreenshot to change filename date format */
        findAndHookMethod(
            "com.android.systemui.screenshot.SaveImageInBackgroundTask",
            lpparam.classLoader,
            "doInBackground",
            emptyArray<Void>()::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val context = AndroidAppHelper.currentApplication().baseContext

                    val imageTime = getLongField(param.thisObject, "mImageTime")
                    val imageDate =
                        SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)
                            .format(Date(imageTime))
                    val imageFileName = getScreenshotFilename(context, imageDate, "png")
                    setObjectField(param.thisObject, "mImageFileName", imageFileName)
                }
            })
    }
}

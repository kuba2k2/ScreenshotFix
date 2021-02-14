package pl.szczodrzynski.screenshotfix

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import pl.szczodrzynski.screenshotfix.Utils.TAKE_SCREENSHOT_FULLSCREEN
import pl.szczodrzynski.screenshotfix.Utils.TAKE_SCREENSHOT_SELECTED_REGION

class FrameworkHook : IXposedHookLoadPackage {

    private var screenshotTaken = false

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam?.packageName != "android")
            return

        /* hook the framework's PhoneWindowManager to enable partial screenshot on short click */
        /* ported from EvolutionX Android 11 */
        /* https://github.com/Evolution-X/frameworks_base/blob/elle/services/core/java/com/android/server/policy/PhoneWindowManager.java#L4149 */
        /* https://github.com/Evolution-X/frameworks_base/commit/4227e90a2ff02cfef4af3a1fc7ae8004cf655df2 */
        findAndHookMethod(
            "com.android.server.policy.PhoneWindowManager",
            lpparam.classLoader,
            "cancelPendingScreenshotChordAction",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val volumeDownTriggered =
                        getBooleanField(param.thisObject, "mScreenshotChordVolumeDownKeyTriggered")
                    val volumeDownConsumed =
                        getBooleanField(param.thisObject, "mScreenshotChordVolumeDownKeyConsumed")
                    val volumeUpTriggered =
                        getBooleanField(param.thisObject, "mA11yShortcutChordVolumeUpKeyTriggered")

                    val screenshotType = getIntField(param.thisObject, "mVolButtonScreenshotType")

                    /*XposedBridge.log(
                        "volumeDownTriggered = $volumeDownTriggered, " +
                                "volumeDownConsumed = $volumeDownConsumed, " +
                                "volumeUpTriggered = $volumeUpTriggered, " +
                                "screenshotType = $screenshotType, " +
                                "screenshotTaken = $screenshotTaken"
                    )*/

                    if (!volumeDownTriggered && volumeDownConsumed && !volumeUpTriggered) {
                        if (screenshotTaken) {
                            // return if long pressed before (taken a default type screenshot)
                            screenshotTaken = false
                            return
                        }
                        //XposedBridge.log("SHORT CLICK DETECTED")
                        callMethod(
                            param.thisObject,
                            "takeScreenshot",
                            if (screenshotType == 1) TAKE_SCREENSHOT_FULLSCREEN else TAKE_SCREENSHOT_SELECTED_REGION
                        )
                    }
                }
            })

        /* hook the screenshot runnable to prevent doubled screenshots when long pressing */
        findAndHookMethod(
            "com.android.server.policy.PhoneWindowManager\$ScreenshotRunnable",
            lpparam.classLoader,
            "run",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // get the default screenshot type
                    val screenshotType = getIntField(
                        getSurroundingThis(param.thisObject),
                        "mVolButtonScreenshotType"
                    )
                    val defaultScreenshotType =
                        if (screenshotType != 1) TAKE_SCREENSHOT_FULLSCREEN else TAKE_SCREENSHOT_SELECTED_REGION
                    val thisScreenshotType = getIntField(param.thisObject, "mScreenshotType")
                    //XposedBridge.log("defaultScreenshotType = $defaultScreenshotType, thisScreenshotType = $thisScreenshotType")
                    // compare with this runnable`s screenshot type
                    // mark screenshotTaken only when long pressed
                    screenshotTaken = thisScreenshotType == defaultScreenshotType
                }
            })
    }
}

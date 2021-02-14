# ScreenshotFix

An Xposed module.

Various fixes for the ASUS StitchImage screenshot service.

This module:
- makes the screenshot editor respect the `screenshot_format` setting from `system` namespace (`0` for JPG, `1` for PNG)
- makes the screenshot editor replace the original file instead of adding `File (1).png`
- changes the filename date format to `YYYY-MM-DD_HH-MM-SS`
- appends the current application name to the filename
- enables capturing partial screenshot on short Power+VolDown click (or full screenshot, if partial enabled as default)
- enables editor for screenshots taken in the app launcher

The module is tested on Resurrection Remix 8.6.1 (Android 10) and will probably not work on any other custom OS/Android version.

### How to enable PNG screenshots?

```shell script
adb shell settings put system screenshot_format 1 # or 0 for JPG 
```

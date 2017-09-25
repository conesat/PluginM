#!/bin/sh
adb shell mkdir /sdcard/PluginM/
./gradlew pluginlib:clean pluginlib:makeJar && ./gradlew plugintest1:clean plugintest1:assembleDebug && adb push plugintest1/build/outputs/apk/plugintest1-debug.apk /sdcard/PluginM/ && ./gradlew testplugin:clean testplugin:assembleDebug && adb push testplugin/build/outputs/apk/testplugin-debug.apk /sdcard/PluginM/ && ./gradlew testhost:clean testhost:assembleDebug  && adb push testhost/build/outputs/apk/testhost-debug.apk /data/local/tmp/com.example.testhost && adb shell pm install -r "/data/local/tmp/com.example.testhost" && adb shell am start -n "com.example.testhost/com.example.testhost.DemoActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
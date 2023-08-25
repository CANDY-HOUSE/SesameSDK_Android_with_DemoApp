#!/usr/bin/env bash

PACKAGE=co.candyhouse.sesame2
ACTIVITY=co.candyhouse.app.tabs.MainActivity
APK_LOCATION=app-release.apk
# echo "Package: $PACKAGE"

# echo "Building the project with tasks: $TASKS"
# ./gradlew $TASKS

# echo "Uninstalling $PACKAGE"
# adb uninstall $PACKAGE

echo "Installing $APK_LOCATION"
adb install $APK_LOCATION

echo "Starting $ACTIVITY"
adb shell am start -n $PACKAGE/$ACTIVITY


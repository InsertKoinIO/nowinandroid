#!/bin/bash

# Variables
APK_PATH="./build/outputs/apk/demo/release/app-demo-release.apk"
PACKAGE_NAME="com.google.samples.apps.nowinandroid.demo"
MAIN_ACTIVITY="com.google.samples.apps.nowinandroid.MainActivity"
OUTPUT_FILE="benchmark_log.txt"
NUM_LOOPS=10  # Number of times to loop
WAIT_TIME=5   # Time to wait in seconds between each loop iteration

# Step 1: Clean and assemble the APK
echo "Running Gradle clean and assemble..."
../gradlew clean assembleDemoRelease

# Step 2: Uninstall the app if it's already installed
echo "Uninstalling the app from the device..."
adb uninstall $PACKAGE_NAME

# Step 3: Install the new APK
echo "Installing the APK on the device..."
adb install $APK_PATH

# Loop over Step 4 and Step 5, NUM_LOOPS times with a WAIT_TIME-second wait in between
for ((i = 1; i <= NUM_LOOPS; i++))
do
  # Step 4: Launch the app's main activity
  echo "Starting the app... (Iteration $i)"
  adb shell am start -n $PACKAGE_NAME/$MAIN_ACTIVITY

  # Wait for WAIT_TIME seconds
  sleep $WAIT_TIME

  # Step 5: Force-stop the app
  echo "Stopping the app... (Iteration $i)"
  adb shell am force-stop $PACKAGE_NAME
done

# Step 6: Extract the log file from the app's internal storage
echo "Retrieving the log file..."
adb shell run-as $PACKAGE_NAME cat /data/user/0/$PACKAGE_NAME/files/$OUTPUT_FILE > $OUTPUT_FILE

echo "Log file saved to $OUTPUT_FILE"

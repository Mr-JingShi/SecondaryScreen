# jar包执行步骤

cd /x/y/z/SecondaryScreen/server/build/intermediates/apk/debug

cp server-debug.apk secondaryscreen-server-debug.jar

adb push secondaryscreen-server-debug.jar /data/local/tmp/

Android 10 ～ 12需要指定Activity

adb shell CLASSPATH=/data/local/tmp/overlaywindow-server-debug.jar app_process / com.secondaryscreen.server.Server A.B.C/A.B.C.FirstActivity A.B.C/A.B.C.SecondActivity

Android 13以及以上不需要指定Activity

adb shell CLASSPATH=/data/local/tmp/secondaryscreen-server-debug.jar app_process / com.secondaryscreen.server.Server

或者nphup方式启动

adb shell CLASSPATH=/data/local/tmp/secondaryscreen-server-debug.jar nohup app_process / com.secondaryscreen.server.Server A.B.C/A.B.C.FirstActivity A.B.C/A.B.C.SecondActivity  >/dev/null 2>&1 &

adb shell CLASSPATH=/data/local/tmp/secondaryscreen-server-debug.jar nohup app_process / com.secondaryscreen.server.Server >/dev/null 2>&1 &

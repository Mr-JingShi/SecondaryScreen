# jar包执行步骤

第一步：修改server目录下build.gradle文件

```groovy
plugins {
    id 'com.android.library'
}
修改为
plugins {
    id 'com.android.application'
}
```

```groovy
defaultConfig {
    namespace "com.secondaryscreen.server"
}
添加applicationId "com.secondaryscreen.server"
defaultConfig {
    // applicationId "com.secondaryscreen.server"
    namespace "com.secondaryscreen.server"
}
```

第二步：删除server路径下的build文件夹

第三步：重新编译server

cd /x/y/z/SecondaryScreen/server/build/build/outputs/apk

cp server-debug.apk secondaryscreen-server-debug.jar

adb push secondaryscreen-server-debug.jar /data/local/tmp/

Android 10 ～ 12需要指定Activity

adb shell CLASSPATH=/data/local/tmp/overlaywindow-server-debug.jar app_process / com.secondaryscreen.server.Server A.B.C/A.B.C.FirstActivity A.B.C/A.B.C.SecondActivity

Android 13以及以上不需要指定Activity

adb shell CLASSPATH=/data/local/tmp/secondaryscreen-server-debug.jar app_process / com.secondaryscreen.server.Server

或者nohup方式启动

adb shell CLASSPATH=/data/local/tmp/secondaryscreen-server-debug.jar nohup app_process / com.secondaryscreen.server.Server A.B.C/A.B.C.FirstActivity A.B.C/A.B.C.SecondActivity >/dev/null 2>&1 &

adb shell CLASSPATH=/data/local/tmp/secondaryscreen-server-debug.jar nohup app_process / com.secondaryscreen.server.Server >/dev/null 2>&1 &

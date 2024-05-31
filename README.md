OverlayWindow 好用的副屏模拟器

# 背景

最近几年新能源电车大火，华为等不少厂商推出了副驾屏，不少车机应用开始支持副驾屏，用以实现主副屏联动。开发工程师可通过Android手机开发者模式中的“绘图-模拟辅助显示“模拟副屏，此时主屏会有一个副屏叠加窗，然副屏无法进行触摸操作，我们可以通过scrcpy把副屏映射到电脑上进行相关操作。

# srccpy映射副屏

## 注意事项

支持Android 10及以上机型

## 手动或shell创建副屏

shell命令创建一块副屏

$settings put global overlay_display_devices "WxH/DENSITY"

shell命令创建多块副屏（最多6块）

$settings put global overlay_display_devices "WxH/DENSITY,secure;WxH/DENSITY,secure"

shell命令删除副屏

$settings put global overlay_display_devices "null"

$settings put global overlay_display_devices ""

$settings delete global overlay_display_devices

## scrcpy查看副屏displayid

$scrcpy --list-displays

```shell
scrcpy 2.4 <https://github.com/Genymobile/scrcpy>
INFO: ADB device found:
INFO:     -->   (usb)  HT6BG0200763                    device  AOSP_on_msm8996
/usr/local/Cellar/scrcpy/2.4/share/scrcpy/scrcpy-server: 1 file pushed, 0 skipped. 61.7 MB/s (69007 bytes in 0.001s)
[server] INFO: Device: [Google] Android AOSP on msm8996 (Android 10)
[server] INFO: List of displays:
--display-id=0    (2560x1440)
--display-id=14    (2392x1440)
```

## scrcpy映射副屏

$scrcpy --display-id=14

## 副屏弹出软键盘

部分手机无法弹出软键盘，可通过打开Android手机开发者模式中的“绘图-强制桌面模式“解决，或者使用电脑键盘代替。

shell命令也可打开、关闭强制桌面模式。

### 查看强制桌面模式

$settings get global force_desktop_mode_on_external_displays

### 打开强制桌面模式

$settings put global force_desktop_mode_on_external_displays 1

### 关闭强制桌面模式

$shell settings put global force_desktop_mode_on_external_displays 0

# scrcpy映射副屏存在的问题

scrcpy映射副屏方案能够解决大部分问题，但大部分电脑屏幕是非触摸屏，只可以用鼠标进行操作，鼠标只能模拟单指，无法模拟出双指、多指操组。且系统Overlay方式始终在主屏上有一个遮盖的叠加窗，这个叠加窗无法操作，更无法隐藏，因此在手机上开发一个可操作可隐藏的自定义悬浮窗迫在眉睫。

# App创建副屏

目标：支持单指、双指、多指触摸，可弹出软键盘，可隐藏，可scrcpy映射。

叠加窗逻辑参考自:

https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/display/OverlayDisplayWindow.java

注入事件逻辑参考自：

https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/wrappers/InputManager.java

shell命令逻辑参考自：

https://github.com/gtf35/app_process-shell-use/blob/master/app/src/main/java/shellService/ServiceShellUtils.java

## 注意事项

支持Android 10及以上机型

因App无Android系统级权限，所以OverlayWindow App创建VirtualDisplay无法在另一个App中使用，此时会报权限错误。然可通过shell命令提权，使用am start命令启动secondActivity，并通过--display参数指定到副屏上，这样就能让secondActivity运行在OverlayWindow App创建的副屏上。 

业务App需要过滤掉OverlayWindow App创建的VirtualDisplay（如过滤VirtualDisplay的名称），否则业务App无法正常运行。然后通过scrcpy方式验证业务App正常启动secondActivity的逻辑，这样就能覆盖OverlayWindow App无法验证的部分（shell命令启动secondActivity）。

因此scrcpy+OverlayWindow App搭配才能覆盖所有业务逻辑。

## 整体架构

### demo

悬浮窗：权限、启动、touch等

virtualDisplay：创建，绑定surface等

socketCliet：与jar包通信，主要传递x、y转换后的touch事件

#### app启动

正常app启动即可

### server

socketServer：与app通信，接受touch事件

injectEevent：完成touch事件注入

detectThread：检测并启动副屏activity

#### jar包执行步骤

cd /x/y/z/OverlayWindow/server/build/intermediates/apk/debug

cp server-debug.apk overlaywindow-server-debug.jar

adb push overlaywindow-server-debug.jar /data/local/tmp/

adb shell CLASSPATH=/data/local/tmp/overlaywindow-server-debug.jar app_process / com.overlaywindow.server.Server A.B.C/A.B.C.MainActivity A.B.C/A.B.C.SecondActivity

或者nphup方式启动

adb shell CLASSPATH=/data/local/tmp/overlaywindow-server-debug.jar nohup app_process / com.overlaywindow.server.Server A.B.C/A.B.C.MainActivity A.B.C/A.B.C.SecondActivity >/dev/null 2>&1 &
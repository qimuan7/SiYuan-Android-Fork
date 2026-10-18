# SiYuan-Android-Fork

思源筆記安卓端修改, 使其支援自定工作區文件夾

此倉庫包含 release 下載, 源碼以及自構建指南, 只是自用心血來潮改一下, 擔心安全請使用原版

---

---

### 構建:

- 從 Assets-Build-Kernel / Build-Directly 下載 app.zip 和 nernel.aar

1. 這是我的構建, app.zip 來自官方 siyuan-3.8.7.apk 解壓

2. kernel 對已下文件修改, 使原放在 android / data / org.b3log.siyuan 下的工作區可以被指定放在用戶目錄, 並將工作區中的 home/ 目錄 (對應windows的 user/.config/siyuan/) 放到應用私有目錄內
> kernel / mobile / kernel.go
> kernel / util / working_mobile.go

- 接下來

1. app.zip 放到你的源碼中的 SiYuan-android/app/src/main/assets

2. kernel.aar 放到你的源碼中的 SiYuan-android/app/libs

剩下的 ui 可以自己寫, 也可以把倉庫裏的 App.java ; BootActivity.java ; JSAndroid.java ; MainActivity.java ; WelcomeActivity.java 作爲參考

Directory structure reference:

![project-tree](project-tree.png)

![app.zip](app-zip.png)

## About Multi-Channel Software Distribution

If you are building your program using the Android Studio method of going to 【Build】,【Generate Signed Bundle APK...】, you only need to modify the `siyuanVersionName` and `siyuanVersionCode` within the build.gradle file at the project level. After making the changes, you can directly package the app and ignore the following content.

### Steps

**The following content is only necessary when building via the command line console**:

When building using the command line console, you not only need to modify the `siyuanVersionName` and `siyuanVersionCode` within the build.gradle file at the project level, but you also need to perform the following steps:

1. Copy the `signings.templates.gradle` file and rename it to `signings.gradle`.
2. Configure the related information in `signings.gradle`.
3. Use the command line to navigate to the root directory of the project and execute the following
   ```shell
   # windows
   .\gradlew clean buildReleaseTask
   # linux
   gradle clean buildReleaseTask
   ```
   
   The naming convention is as follows:

   ```txt
   assemble/bundle  Googleplay  Debug/Release
   ```
   
   `assemble` generates APKs
   `bundle` generates AABs
   `Googleplay` is the name of the channel package; refer to the `productFlavors {}` configuration in flavors.gradle for the specified location
   `Debug/Release` stands for Test version/Official version
4. After the execution is complete, you can find the generated program at the following location
   ```txt
   siyuan-android\app\build-release\siyuan-${versionName}-all
   ```

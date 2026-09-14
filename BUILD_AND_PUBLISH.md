# راهنمای بیلد، اجرا و انتشار افزونه

## پیش‌نیازها

- **JDK 17** یا بالاتر (IntelliJ Platform 2023.3+ برای کامپایل به JDK 17 نیاز دارد).
- **Gradle** (یا صرفاً Gradle Wrapper بعد از یک‌بار تولید — بخش پایین همین فایل را ببینید).
- IntelliJ IDEA کاملاً **اختیاری** است؛ فقط برای راحتیِ کدنویسی پیشنهاد می‌شود، نه بیلد.

## ۱. تولید Gradle Wrapper (فقط یک‌بار)

فایل‌های `gradlew`/`gradlew.bat` در این پروژه از قبل وجود ندارند. یک‌بار Gradle را نصب کنید
(Chocolatey: `choco install gradle -y`، یا Scoop: `scoop install gradle`، یا دانلود مستقیم از
gradle.org) و داخل پوشه‌ی پروژه این را اجرا کنید:

```powershell
gradle wrapper --gradle-version 8.10
```

این فایل‌ها را در Git commit کنید تا دیگر لازم نباشد تکرار شود.

## ۲. اجرا و تست محلی

```powershell
.\gradlew.bat runIde
```

این دستور یک نمونه‌ی sandbox از Rider با افزونه‌ی از قبل نصب‌شده باز می‌کند تا رفتار واقعی
افزونه (کلیک‌پذیر شدن `in file.cs:line N`) را در یک پروژه‌ی نمونه تست کنید.

## ۳. ساخت فایل نصب (zip)

```powershell
.\gradlew.bat buildPlugin
```

خروجی: `build\distributions\rider-demystified-links-1.0.0.zip`

## ۴. تکمیل اطلاعات نمایشی (توضیحات، آیکون، یادداشت انتشار)

همان‌طور که در README توضیح داده شد، این‌ها همگی از `src/main/resources/META-INF/plugin.xml`
خوانده می‌شوند: `<description>`, `<change-notes>`, `<vendor>`. برای هر آپدیت، این تگ‌ها را
دستی ویرایش کنید و دوباره `buildPlugin` بزنید — فرم جداگانه‌ای روی سایت برای این متن‌ها وجود
ندارد.

## ۵. انتشار — نسخه‌ی اول (باید دستی باشد)

1. با حساب JetBrains وارد [plugins.jetbrains.com](https://plugins.jetbrains.com) شوید.
2. از پروفایل خود: **Add new plugin** → فایل zip مرحله‌ی ۳ را آپلود کنید.
3. تیم JetBrains نسخه‌ی اول را دستی بررسی می‌کند (چند روز طول می‌کشد).

> مهم: اولین انتشار هر افزونه‌ی جدید همیشه باید از طریق وب‌سایت انجام شود؛ دستور
> `publishPlugin` فقط برای **نسخه‌های بعدی** یک افزونه‌ی از قبل تأییدشده کار می‌کند.

## ۶. گرفتن و ذخیره‌ی توکن انتشار (برای نسخه‌های بعدی)

1. در پروفایل Marketplace: بخش **My Tokens** → **Generate Token**.
2. در PowerShell (برای همیشه):

```powershell
setx PUBLISH_TOKEN "perm:xxxxxxxxxxxxxxxxxxxx"
```

یک ترمینال جدید باز کنید تا مقدار بارگذاری شود.

## ۷. انتشار نسخه‌های بعدی (کاملاً خودکار)

```powershell
# شماره pluginVersion را در gradle.properties افزایش دهید، سپس:
.\gradlew.bat publishPlugin
```

## ۸. امضای دیجیتال (اختیاری)

```powershell
$env:CERTIFICATE_CHAIN = Get-Content chain.crt -Raw
$env:PRIVATE_KEY = Get-Content private.pem -Raw
$env:PRIVATE_KEY_PASSWORD = "..."
.\gradlew.bat signPlugin
```

`build.gradle.kts` این متغیرها را از قبل می‌خواند، پس `signPlugin` قبل از `publishPlugin`
به‌طور خودکار اجرا می‌شود.

## منابع رسمی

- [Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [Best Practices for Listing](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html)
- [Plugin Icon File](https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html)
- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)

# راهنمای بیلد، اجرا و انتشار افزونه

## پیش‌نیازها

- **JDK 17** یا بالاتر.
- **Gradle 8.x** (نه 9؛ پلاگین `org.jetbrains.intellij` نسخه‌ی 1.17.4 با Gradle 9 ناسازگاره
  چون از یک API داخلی حذف‌شده به اسم `DefaultArtifactPublicationSet` استفاده می‌کنه).
- IntelliJ IDEA کاملاً اختیاریه؛ فقط برای راحتیِ کدنویسی.

## ۱. تولید Gradle Wrapper (فقط یک‌بار)

```powershell
choco install gradle --version 8.10 -y
cd D:\Local\rider-demystified-links
gradle wrapper --gradle-version 8.10
```

اگه با خطای resolve پلاگین یا SSL مواجه شدید، اول در یک پوشه‌ی کاملاً خالی امتحان کنید تا
مطمئن بشید مشکل شبکه‌ست نه پروژه. فایل‌های تولیدشده (`gradlew`, `gradlew.bat`, `gradle/`) رو
حتماً commit کنید.

## ۲. بیلد بدون دانلود سنگین روی سیستم شخصی (توصیه‌شده)

اگه دانلود چند گیگابایتی IDE هدف روی اینترنت شخصی‌تون طول می‌کشه، از GitHub Actions استفاده
کنید (فایل `.github/workflows/build.yml` از قبل آماده‌ست):

1. پروژه رو به یک ریپازیتوری گیت‌هاب پوش کنید.
2. تب **Actions** → **Build Plugin** → **Run workflow**.
3. بعد از چند دقیقه، zip نهایی در بخش **Artifacts** قابل دانلوده.

## ۳. بیلد محلی (اگه اینترنت پرسرعت دارید)

```powershell
.\gradlew.bat buildPlugin
```

خروجی: `build\distributions\rider-demystified-links-1.1.0.zip`

## ۴. تست محلی

```powershell
.\gradlew.bat runIde
```

یک Rider sandbox باز می‌شه؛ برای تست هر دو ویژگی:

- یک لاگ Demystifier در کنسول Run/Debug چاپ کنید و چک کنید `in file.cs:line N` کلیک‌پذیره.
- یک استک‌تریس رو کپی کنید (Ctrl+C روی متنی که `Exception` و یک خط `at ...` داره) و ببینید
  پنجره‌ی Stacktrace خودش باز می‌شه یا نه.

## ۵. تنظیم سازگاری نسخه

`pluginSinceBuild`/`pluginUntilBuild` در `gradle.properties` هستن. `pluginUntilBuild` رو
عمداً خالی گذاشتیم تا با نسخه‌های آینده‌ی Rider هم کار کنه؛ اگه بعداً از یک API ناپایدار
استفاده کردید، باید این مقدار رو محدود کنید.

## ۶. انتشار — نسخه‌ی اول (دستی)

1. با حساب JetBrains وارد [plugins.jetbrains.com](https://plugins.jetbrains.com) شوید.
2. **Add new plugin** → zip مرحله‌ی ۳ یا ۲ رو آپلود کنید.
3. تیم JetBrains دستی بررسی می‌کنه (چند روز).

> اولین انتشار همیشه دستی از سایته؛ `publishPlugin` فقط برای نسخه‌های بعدیه.

## ۷. گرفتن و ذخیره‌ی توکن

```powershell
setx PUBLISH_TOKEN "perm:xxxxxxxxxxxxxxxxxxxx"
```
(از My Tokens در پروفایل Marketplace می‌گیرید. یک ترمینال جدید باز کنید تا لود بشه.)

## ۸. انتشار نسخه‌های بعدی

```powershell
# pluginVersion رو در gradle.properties افزایش بدید، سپس:
.\gradlew.bat publishPlugin
```

## ۹. امضای دیجیتال (اختیاری)

```powershell
$env:CERTIFICATE_CHAIN = Get-Content chain.crt -Raw
$env:PRIVATE_KEY = Get-Content private.pem -Raw
$env:PRIVATE_KEY_PASSWORD = "..."
.\gradlew.bat signPlugin
```

## عیب‌یابی مشکلات رایج

| خطا | راه‌حل |
|---|---|
| `gradlew.bat not recognized` | باید `.\gradlew.bat` بزنید (با پیشوند `.\`)، و مطمئن بشید wrapper از قبل تولید شده. |
| `could not resolve plugin artifact` | مشکل شبکه/پروکسی؛ در پوشه‌ی خالی تست کنید، یا `-Djava.net.useSystemProxies=true` رو امتحان کنید. |
| `Trust store file NUL does not exist` | `systemProp.javax.net.ssl.trustStore=NUL` رو از `gradle.properties` حذف کنید؛ به‌جاش از `Windows-ROOT` استفاده کنید (در همین پروژه از قبل تنظیم شده). |
| `DefaultArtifactPublicationSet not present` | نسخه‌ی Gradle شما 9.x هست؛ باید 8.10 نصب کنید. |
| `not compatible ... requires build 253.* or older` | `pluginUntilBuild` رو در `gradle.properties` خالی بگذارید (در این پروژه از قبل انجام شده). |

## منابع رسمی

- [Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [Build Number Ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html)
- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)

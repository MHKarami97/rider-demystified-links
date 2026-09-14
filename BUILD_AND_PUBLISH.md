# راهنمای بیلد، اجرا و انتشار افزونه

## پیش‌نیازها

- **JDK 17** یا بالاتر (IntelliJ Platform 2023.3+ برای کامپایل به JDK 17 نیاز دارد).
- **Gradle** (یا صرفاً Gradle Wrapper بعد از یک‌بار تولید — بخش پایین همین فایل را ببینید).
- IntelliJ IDEA کاملاً **اختیاری** است؛ فقط برای راحتیِ کدنویسی پیشنهاد می‌شود، نه بیلد.
- اتصال اینترنت برای اولین اجرای Gradle، چون باید Rider IDE به‌عنوان "پلتفرم هدف" دانلود شود
  (مگر اینکه از قبل روی سیستم نصب باشد و مسیرش را در `build.gradle.kts` مشخص کنید).

## ۱. باز کردن پروژه (اختیاری، در IntelliJ IDEA)

1. پوشه‌ی `rider-demystified-links` را در IntelliJ IDEA باز کنید (`File | Open`).
2. IDE به‌طور خودکار Gradle sync را اجرا می‌کند. صبر کنید تا تمام وابستگی‌ها دانلود شوند.
3. اگر پرامپت "Trust Project" ظاهر شد، آن را تأیید کنید.

## ۲. اجرا و تست محلی (بدون انتشار)

برای اجرای یک نمونه‌ی Rider با افزونه از قبل نصب‌شده (sandbox instance):

```bash
./gradlew runIde
```

> نکته: چون در `gradle.properties` مقدار `platformType = RD` تنظیم شده، Gradle IntelliJ Plugin
> به‌جای IntelliJ IDEA، خودِ Rider را به‌عنوان پلتفرم هدف دانلود و اجرا می‌کند. این دانلود اولیه
> ممکن است چند دقیقه طول بکشد.

پس از بالا آمدن Rider sandbox، یک پروژه‌ی دات‌نتی نمونه باز کنید، یک لاگ حاوی
`Serilog.Enrichers.Demystifier` را در کنسول Run/Debug چاپ کنید و بررسی کنید که بخش
`in file.cs:line N` اکنون آبی و کلیک‌پذیر است.

## ۳. ساخت فایل نصب (build) برای نصب دستی

```bash
./gradlew buildPlugin
```

خروجی در مسیر زیر ساخته می‌شود:

```
build/distributions/rider-demystified-links-1.0.0.zip
```

### نصب دستی این فایل در Rider خودتان

1. در Rider: `Settings/Preferences | Plugins`
2. آیکون چرخ‌دنده (⚙️) کنار نوار جستجو → `Install Plugin from Disk...`
3. فایل zip بالا را انتخاب کنید و Rider را ری‌استارت کنید.

## ۴. تنظیم نسخه‌ی هدف (Since/Until Build)

قبل از انتشار عمومی، مقادیر زیر را در `gradle.properties` با نسخه‌ی واقعی Rider که تست کرده‌اید
هماهنگ کنید (شماره build را از `Help | About` در Rider می‌گیرید):

```properties
pluginSinceBuild = 233
pluginUntilBuild = 253.*
```

## ۵. انتشار در JetBrains Marketplace

### مرحله‌ی ۱ — ساخت حساب و دریافت توکن

1. با حساب JetBrains به [plugins.jetbrains.com](https://plugins.jetbrains.com) وارد شوید.
2. از پروفایل خود، بخش **My Tokens** یک Permanent Token جدید بسازید.
3. توکن را در متغیر محیطی ذخیره کنید (هرگز داخل کد commit نکنید):

```bash
export PUBLISH_TOKEN="your-token-here"
```

### مرحله‌ی ۲ — (اختیاری ولی توصیه‌شده) امضای افزونه

JetBrains Marketplace امضای دیجیتال افزونه را برای اعتبارسنجی توصیه می‌کند:

```bash
export CERTIFICATE_CHAIN="$(cat chain.crt)"
export PRIVATE_KEY="$(cat private.pem)"
export PRIVATE_KEY_PASSWORD="your-password"
./gradlew signPlugin
```

راهنمای کامل ساخت گواهی: [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)

### مرحله‌ی ۳ — انتشار

```bash
./gradlew publishPlugin
```

این دستور فایل zip بیلدشده را مستقیماً با استفاده از `PUBLISH_TOKEN` به Marketplace ارسال می‌کند.
اولین انتشار هر افزونه توسط تیم JetBrains به‌صورت دستی بررسی می‌شود (چند روز طول می‌کشد)؛
نسخه‌های بعدی معمولاً سریع‌تر تأیید می‌شوند.

### مرحله‌ی ۴ — انتشار به‌صورت خصوصی (اختیاری)

اگر نمی‌خواهید افزونه عمومی باشد، می‌توانید فایل zip حاصل از `buildPlugin` را در یک
[Custom Plugin Repository](https://plugins.jetbrains.com/docs/intellij/update-plugins-format.html)
داخلی (مثلاً یک فایل استاتیک روی وب‌سرور خودتان) میزبانی کنید و آدرسش را در Rider تحت
`Settings | Plugins | ⚙️ | Manage Plugin Repositories` اضافه کنید.

---

## بیلد فقط با ترمینال یا VS Code (بدون IntelliJ IDEA)

IntelliJ IDEA در بخش قبل صرفاً برای راحتیِ کدنویسی (تکمیل خودکار، ناوبری بین فایل‌ها) پیشنهاد شده
بود؛ خودِ فرآیند بیلد کاملاً بر پایه‌ی Gradle CLI است و به هیچ IDE خاصی وابسته نیست.

### ۱. یک‌بار Gradle Wrapper را تولید کنید

فایل‌های `gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar` در پروژه‌ی اولیه وجود
ندارند (چون تولید باینری jar آن‌ها در محیط تولید این فایل‌ها امکان‌پذیر نبود). یک‌بار Gradle را
نصب کنید (مثلاً با `sdk install gradle` در SDKMAN، یا `choco install gradle` در ویندوز، یا دانلود
مستقیم از gradle.org) و داخل پوشه‌ی پروژه این دستور را اجرا کنید:

```bash
gradle wrapper --gradle-version 8.10
```

این کار wrapper را می‌سازد و از این به بعد دیگر نیازی به Gradle نصب‌شده روی سیستم نیست؛
`./gradlew` خودش نسخه‌ی درست را دانلود و مدیریت می‌کند. این فایل‌ها را در ریپازیتوری Git خودتان
commit کنید تا همکاران هم بدون نصب دستی Gradle بتوانند بیلد بگیرند.

### ۲. ویرایش کد در VS Code

برای IntelliSense و ناوبری کد Kotlin در VS Code، این افزونه‌ها را نصب کنید:

- **Kotlin by JetBrains** (`jetbrains.kotlin-server`): language server رسمی JetBrains برای Kotlin.
- **Gradle for Java** (`vscjava.vscode-gradle`): نمایش گرافیکی Task های Gradle و اجرای آن‌ها
  به‌عنوان VS Code Task، بدون نیاز به تایپ دستی هر بار.

نصب این‌ها اختیاری است؛ صرفاً تجربه‌ی ادیت را بهتر می‌کند و هیچ تأثیری روی خروجی بیلد ندارد.

### ۳. بیلد و اجرا فقط با ترمینال

```bash
# بیلد فایل نصب (zip)
./gradlew buildPlugin

# اجرای یک نمونه‌ی sandbox رایدر برای تست دستی افزونه
./gradlew runIde

# انتشار مستقیم در Marketplace (بعد از export کردن PUBLISH_TOKEN)
./gradlew publishPlugin
```

نکته: دستور `runIde` هنوز یک پنجره‌ی واقعی Rider (نه VS Code) باز می‌کند تا افزونه را تست کنید،
چون افزونه در نهایت باید داخل خودِ Rider اجرا شود؛ اما نوشتن و بیلد گرفتن کد کاملاً از طریق
ترمینال یا VS Code ممکن است — IntelliJ IDEA در هیچ مرحله‌ای اجباری نیست.

## منابع رسمی

- [Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- [Gradle IntelliJ Plugin — GitHub](https://github.com/JetBrains/gradle-intellij-plugin)
- [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [Build Number Ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html)
- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
- [Gradle in IDEs — VS Code support](https://docs.gradle.org/current/userguide/gradle_ides.html)
- [Java and Kotlin by IntelliJ IDEA (VS Code extension)](https://www.jetbrains.com/help/intellij-vscode/get_started_vs_code.html)

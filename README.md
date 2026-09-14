# Demystified Stack Trace Links — Rider Plugin

## این افزونه چیست؟

این افزونه یک مشکل مشخص در JetBrains Rider را حل می‌کند: وقتی از `Serilog.Enrichers.Demystifier`
(یعنی گزینه‌ی `WithDemystifiedStackTraces` در تنظیمات Serilog) استفاده می‌کنید، فرمت استک‌تریس‌های
چاپ‌شده در کنسول با گرامر استانداردی که Stack Trace Explorer داخلی Rider می‌شناسد فرق می‌کند
(مثلاً پیشوند نوع بازگشتی `async Task<...>` قبل از نام متد، نام‌گذاری‌های
`+<>c__DisplayClass...g__Method|0(?)` برای closure/local function، و پسوند تکرار فریم `x N`).
در نتیجه این خطوط در پنجره‌های Run/Debug/Terminal دیگر کلیک‌پذیر نیستند.

این افزونه یک `ConsoleFilter` سبک اضافه می‌کند که به‌جای تلاش برای فهمیدن کل امضای پیچیده‌ی متد،
فقط دنبال بخش `in <path>:line <N>` می‌گردد — بخشی که در فریم‌های دمیستیفای‌شده هم دست‌نخورده باقی
می‌ماند — و همان بخش را به یک لینک قابل کلیک برای پرش به فایل و خط مربوطه تبدیل می‌کند.

## چگونه کار می‌کند؟

1. **`DemystifiedStackTraceFilterProvider`**: از extension point رسمی
   `com.intellij.consoleFilterProvider` استفاده می‌کند تا فیلتر را به تمام کنسول‌های
   Run / Debug / Terminal اضافه کند.
2. **`DemystifiedStackTraceFilter`**: هر خط از خروجی کنسول را با یک عبارت باقاعده بررسی می‌کند:

   ```
   in\s+[^\s:]*?([A-Za-z0-9_.\-]+\.(?:cs|vb|fs)):line\s+(\d+)
   ```

3. چون مسیر داخل لاگ معمولاً مسیر مطلق سرور CI است (مثلاً `D:/ag/WCA9/_w/757/s/...`) و روی
   ماشین شما وجود ندارد، افزونه از مسیر مطلق صرف‌نظر می‌کند و فقط **نام فایل** استخراج‌شده را
   با استفاده از `FilenameIndex` داخل سالوشن بازِ فعلی جست‌وجو می‌کند.
4. اگر فایل پیدا شود، یک `OpenFileHyperlinkInfo` روی همان بازه از متن ساخته می‌شود که با کلیک،
   فایل را باز کرده و به شماره خط موردنظر می‌پرد.

## اطلاعات نمایشی افزونه در Marketplace (توضیحات، آیکون، ...)

Marketplace هیچ فیلد جداگانه‌ای برای «توضیحات» یا «یادداشت انتشار» روی خودِ سایت ندارد؛ همه‌ی این
موارد مستقیماً از داخل `src/main/resources/META-INF/plugin.xml` خوانده می‌شوند:

| فیلد نمایشی | تگ در plugin.xml |
|---|---|
| توضیحات کامل افزونه | `<description>` |
| یادداشت هر نسخه (Changelog) | `<change-notes>` |
| نام و لینک/ایمیل سازنده | `<vendor url="..." email="...">` |
| لینک وب‌سایت افزونه | صفت `url` روی خودِ تگ `<idea-plugin>` |
| آیکون (لوگو) افزونه | `pluginIcon.svg` و `pluginIcon_dark.svg` در همان پوشه‌ی META-INF |

برای تغییر توضیحات یا نسخه‌ی جدید، فقط کافیست همین تگ‌ها را در `plugin.xml` ویرایش کنید و دوباره
`buildPlugin`/`publishPlugin` بزنید؛ نیازی به وارد کردن دستی متن در فرم سایت نیست، چون هنگام
آپلود zip این مقادیر به‌طور خودکار استخراج و روی صفحه‌ی افزونه نمایش داده می‌شوند.

قوانین آیکون:

- دقیقاً **۴۰×۴۰ پیکسل**، فرمت SVG (نه PNG/JPG)، ترجیحاً زیر ۲ تا ۳ کیلوبایت.
- فایل `pluginIcon.svg` برای تم روشن و `pluginIcon_dark.svg` برای تم تاریک — هر دو باید داخل
  `src/main/resources/META-INF/` باشند.
- در همین پروژه یک آیکون نمونه‌ی ساده (یک "زیگزاگ" به‌شکل حرف Z که نماد پرش بین فریم‌های
  استک‌تریس است) در همین مسیر گذاشته شده؛ می‌توانید آن را با طرح دلخواه خودتان جایگزین کنید.

## محدودیت‌های شناخته‌شده

- اگر چند فایل هم‌نام (مثلاً دو `Extensions.cs` در پروژه‌های مختلف سالوشن) وجود داشته باشد،
  نسخه‌ی فعلی فقط **اولین نتیجه** را باز می‌کند.
- فقط پسوندهای `.cs`، `.vb` و `.fs` پشتیبانی می‌شوند؛ برای زبان‌های دیگر باید regex را در
  `DemystifiedStackTraceFilter.kt` گسترش دهید.
- این افزونه صرفاً بخش front-end (IntelliJ Platform / JVM) را پوشش می‌دهد و نیازی به
  ReSharper .NET SDK ندارد.

## ساختار پروژه

```
rider-demystified-links/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── README.md                 <- همین فایل
├── BUILD_AND_PUBLISH.md      <- راهنمای بیلد، اجرا و انتشار
└── src/main/
    ├── kotlin/com/mhkarami/riderdemystifiedlinks/
    │   └── DemystifiedStackTraceFilter.kt
    └── resources/META-INF/
        ├── plugin.xml
        ├── pluginIcon.svg
        └── pluginIcon_dark.svg
```

## منابع رسمی

- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Extension Points](https://plugins.jetbrains.com/docs/intellij/plugin-extension-points.html)
- [Rider Plugin Development](https://plugins.jetbrains.com/docs/intellij/rider.html)
- [Best Practices for Listing](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html)
- [Plugin Icon File](https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html)
- [Ben.Demystifier](https://github.com/benaadams/Ben.Demystifier)
- [Serilog.Enrichers.Demystifier](https://github.com/nblumhardt/serilog-enrichers-demystify)

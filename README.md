# Demystified Stack Trace Links — Rider Plugin

## این افزونه چیست؟

این افزونه دو مشکل جدا اما مرتبط رو در JetBrains Rider حل می‌کنه، هر دو ناشی از استفاده از
`Serilog.Enrichers.Demystifier` (گزینه‌ی `WithDemystifiedStackTraces` در Serilog):

1. **کلیک‌ناپذیر بودن فریم‌های استک‌تریس در کنسول Run/Debug/Terminal**: فرمت Demystifier
   (پیشوند نوع بازگشتی `async Task<...>`، نام‌گذاری `+<>c__DisplayClass...g__Method|0(?)`،
   پسوند تکرار فریم `x N`) با گرامر استانداردی که فیلتر داخلی Rider برای این کنسول‌ها استفاده
   می‌کنه فرق داره، پس این خطوط لینک نمی‌شن.
2. **باز نشدن خودکار پنجره‌ی Stacktrace هنگام کپی لاگ**: بر خلاف IntelliJ IDEA، Rider هیچ
   قابلیت مستندی برای اسکن خودکار کلیپ‌بورد نداره؛ پنجره‌ی Analyze Stack Trace همیشه باید
   دستی (از منوی Tools) باز بشه.

نکته‌ی جالب: وقتی همون لاگ رو **دستی** در پنجره‌ی Stack Trace Explorer پیست کنید، Rider خودش
فرمت Demystifier رو درست می‌فهمه و لینک‌ها کار می‌کنن — یعنی پارسر آن پنجره از پارسر کنسول
Run/Debug قوی‌تره. پس مشکل ۱ فقط مخصوص کنسول زنده‌ست، و مشکل ۲ (باز نشدن خودکار) یک قابلیت
جداست که اصلاً وجود نداره و باید از صفر ساخته بشه.

## دو بخش افزونه

### ۱. `DemystifiedStackTraceFilter` — کلیک‌پذیر کردن کنسول زنده

یک `ConsoleFilter` سبک که به‌جای تلاش برای فهمیدن کل امضای پیچیده‌ی متد، فقط دنبال بخش
`in <path>:line <N>` می‌گردد و همان بخش را کلیک‌پذیر می‌کند:

```
in\s+[^\s:]*?([A-Za-z0-9_.\-]+\.(?:cs|vb|fs)):line\s+(\d+)
```

چون مسیر داخل لاگ معمولاً مسیر مطلق سرور CI است، افزونه فقط از روی **نام فایل** آن را در
سالوشن باز جست‌وجو می‌کند (نه از روی مسیر مطلق).

### ۲. `ClipboardStackTraceWatcher` — باز کردن خودکار پنجره‌ی Stacktrace

یک شنونده‌ی کلیپ‌بورد (`CopyPasteManager.ContentsChangedListener`) که هر بار محتوای کلیپ‌بورد
تغییر می‌کند، بررسی می‌کند آیا متن شامل کلمه‌ی `Exception` و حداقل یک خط با شروع `at ` هست یا
نه (همون هیوریستیک شل و کلی‌ای که خودِ IntelliJ IDEA هم استفاده می‌کنه). اگر تشخیص داد، اکشن
داخلی رایدر با آی‌دی `Unscramble` (همون اکشن پشت `Tools | Analyze Stack Trace or Thread Dump`)
رو به‌صورت برنامه‌نویسی‌شده فراخوانی می‌کند تا پنجره‌ی Stacktrace خودکار باز بشه.

**نکته‌ی مهم درباره‌ی این بخش**: چون هیوریستیک عمداً شل هست (برای این‌که فرمت Demystifier رو
هم بگیره)، هر متنی که کلمه‌ی `Exception` و یک خط شبیه `at ...` داشته باشه رو تشخیص می‌ده — even
اگه از StackOverflow یا یک چت کپی کرده باشید. اگه این رفتار زیادی حساس بود، رجکس
`STACK_TRACE_HEURISTIC` در `ClipboardStackTraceWatcher.kt` رو سخت‌گیرانه‌تر کنید.

## ساختار پروژه

```
rider-demystified-links/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── README.md                 <- همین فایل
├── BUILD_AND_PUBLISH.md      <- راهنمای بیلد، اجرا و انتشار
├── .github/workflows/build.yml <- بیلد ابری با GitHub Actions
└── src/main/
    ├── kotlin/com/mhkarami/riderdemystifiedlinks/
    │   ├── DemystifiedStackTraceFilter.kt
    │   └── ClipboardStackTraceWatcher.kt
    └── resources/META-INF/
        ├── plugin.xml
        ├── pluginIcon.svg
        └── pluginIcon_dark.svg
```

## اطلاعات نمایشی افزونه در Marketplace

همه‌چیز (توضیحات، یادداشت انتشار، سازنده) از داخل `plugin.xml` خونده می‌شه، نه از یک فرم جدا
روی سایت:

| فیلد نمایشی | تگ در plugin.xml |
|---|---|
| توضیحات کامل | `<description>` |
| یادداشت هر نسخه | `<change-notes>` |
| نام/ایمیل/لینک سازنده | `<vendor url="..." email="...">` |
| آیکون | `pluginIcon.svg` / `pluginIcon_dark.svg` (دقیقاً ۴۰×۴۰، فرمت SVG) |

## محدودیت‌های شناخته‌شده

- اگر چند فایل هم‌نام در سالوشن باشد، فیلتر فقط اولین نتیجه را باز می‌کند.
- فقط پسوندهای `.cs`، `.vb`، `.fs` پشتیبانی می‌شوند.
- هیوریستیک تشخیص کلیپ‌بورد ممکن است روی متن‌های غیرمرتبط هم false-positive بدهد (بالا توضیح
  داده شد).
- `pluginUntilBuild` عمداً خالی گذاشته شده تا با نسخه‌های آینده‌ی Rider هم سازگار بماند.

## منابع رسمی

- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Extension Points](https://plugins.jetbrains.com/docs/intellij/plugin-extension-points.html)
- [Explore and navigate exception stack traces (Rider)](https://www.jetbrains.com/help/rider/Navigation_and_Search__Navigating_to_Exception.html)
- [Analyze external stack traces (IntelliJ IDEA)](https://www.jetbrains.com/help/idea/analyzing-external-stacktraces.html)
- [Ben.Demystifier](https://github.com/benaadams/Ben.Demystifier)
- [Serilog.Enrichers.Demystifier](https://github.com/nblumhardt/serilog-enrichers-demystify)

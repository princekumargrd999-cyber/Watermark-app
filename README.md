# Video Watermark App

Offline Android app: user apna video chunta hai, app us par user ka icon (bottom-right, fixed)
aur text (ghoomta hua, opacity user ki) laga deta hai. Form ek baar bharo, phone mein save rehta hai.
Sirf banner ad internet use karta hai.

## 1) AdMob IDs kahan likhni hain
File: `app/build.gradle.kts` (bilkul upar)

    val ADMOB_APP_ID = "ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"      // X = 16 digit ~ 10 digit
    val ADMOB_BANNER_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"   // X = 16 digit / 10 digit

- Jab tak `XXXX` likha hai, app Google ke TEST ads dikhayega (app crash nahi hoga).
- Release se pehle dono jagah asli IDs daalo. Apne asli ads par khud click mat karna.

## 2) GitHub se APK banana
1. github.com par naya repository banao.
2. Is zip ko extract karo aur andar ki saari files/folders repo mein upload karo.
3. `.github` hidden folder hai. Agar upload na ho to repo mein "Add file > Create new file" karo,
   naam likho `.github/workflows/build.yml` aur `BUILD_WORKFLOW_COPY.yml` ka content paste karo.
4. Commit karte hi **Actions** tab mein "Build APK" chalega (5-10 minute).
5. Khatam hone par us run ko kholo, neeche **Artifacts > app-debug-apk** download karo,
   zip extract karke `app-debug.apk` phone mein install karo.

## 3) Text ki speed
`WatermarkOverlays.kt` mein `SECONDS_PER_SEGMENT = 4f` (ek side se doosri side tak 4 second). Kam karo to tez.

## Zaroori baatein
- minSdk 29 (Android 10+).
- Output video `Movies/WatermarkApp` mein save hota hai.

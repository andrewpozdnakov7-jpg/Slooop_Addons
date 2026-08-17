# Slooop Google Translation Add-on

This project builds a separately installed Android service for Google ML Kit on-device Translation. It is not included in the Slooop APK and is not used by the F-Droid flavor.

The service accepts calls only from `io.dashchan2` signed with the same certificate. Slooop also checks the add-on package signature and protocol version before sending text.

Build one APK for each supported ABI:

```sh
./gradlew :app:assembleRelease -PaddonAbi=arm64-v8a
./gradlew :app:assembleRelease -PaddonAbi=armeabi-v7a
./gradlew :app:assembleRelease -PaddonAbi=x86
```

The build requires JDK 21 and Android SDK Platform 37. Release APKs must be signed with the same certificate as Slooop. Never commit a signing key or `keystore.properties`.

Release assets use stable ABI-specific names so Slooop can select the correct download automatically:

- `Slooop-Google-Translator-arm64-v8a.apk`
- `Slooop-Google-Translator-armeabi-v7a.apk`
- `Slooop-Google-Translator-x86.apk`

The wrapper source is MIT-licensed. Google ML Kit and downloaded models remain subject to Google's terms, privacy disclosures and Translation attribution requirements.

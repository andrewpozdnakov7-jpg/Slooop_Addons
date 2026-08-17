# Slooop Google Translation Add-on

This project builds a separately installed Android service for Google ML Kit on-device Translation. It is not included in the Slooop APK and is not used by the F-Droid flavor.

The service accepts calls only from `io.dashchan2` signed with the same certificate. Slooop also checks the add-on package signature and protocol version before sending text.

Build one universal APK containing all three Slooop ABIs:

```sh
./gradlew :app:assembleRelease -PaddonAbi=universal
```

The universal APK contains `arm64-v8a`, `armeabi-v7a` and `x86`. ABI-specific
builds remain available through `-PaddonAbi=<abi>` for future split releases.

The build requires JDK 21 and Android SDK Platform 37. Release APKs must be signed with the same certificate as Slooop. Never commit a signing key or `keystore.properties`.

The universal release asset uses the stable name
`Slooop-Google-Translator-universal.apk`.

The wrapper source is MIT-licensed. Google ML Kit and downloaded models remain subject to Google's terms, privacy disclosures and Translation attribution requirements.

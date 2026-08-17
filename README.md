# Slooop Add-ons

Optional resources and separately installed components for Slooop. Add-ons are obtained only after an explicit user action and are not bundled with the application.

Wallpapers and fonts remain passive resource catalogs. Optional executable components are isolated in their own source directories, catalogs and releases. They are never application updates or forum modules, and Slooop must verify their package name, ABI, SHA-256 and signing certificate before use.

The repository currently defines two independent catalogs:

- `wallpapers.json` for optional background images;
- `fonts.json` for optional fonts (the catalog is reserved for future use);
- `translators.json` for separately installed translation services.

The wallpaper catalog currently contains nine optional backgrounds. Users may also select their own local wallpaper without downloading anything from this repository.

The initial wallpaper set was created with generative tools for the Slooop project and is dedicated to the public domain under CC0 1.0. Published PNG files are re-encoded to remove embedded generation prompts, timestamps and local metadata. Small WebP previews are downloaded while browsing the catalog; the full PNG is downloaded only after confirmation.

Catalog URL:

```text
https://raw.githubusercontent.com/andrewpozdnakov7-jpg/Slooop_Addons/main/wallpapers.json
```

## Adding an add-on

Every entry must provide its author, license, dimensions, file size and SHA-256 hash. Only images that can be redistributed under the declared license may be added. Do not commit source prompts, personal metadata or unreviewed third-party images.

Store wallpaper previews in `wallpapers/previews/`, wallpaper files in `wallpapers/images/`, and future fonts in `fonts/files/`. Use stable lowercase identifiers and keep existing files immutable after publication; replace an asset with a new identifier instead.

The wallpaper catalog format is documented and validated by `wallpapers.schema.json`.

## Google translation add-on

`translator-google/` contains the source of an optional Android service that wraps the official Google ML Kit on-device Translation dependency. The add-on is a separate APK so its per-ABI native runtime does not increase the size of the main Slooop APK. Language models are downloaded by ML Kit only after user confirmation.

The add-on wrapper has its own MIT license. Google ML Kit remains subject to Google's terms and attribution requirements. No Google dependency is included in the Slooop F-Droid flavor, and the F-Droid user interface does not offer this add-on.

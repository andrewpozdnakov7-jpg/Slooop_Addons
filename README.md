# Slooop Add-ons

Optional freely licensed resources for Slooop. Add-ons are downloaded only after an explicit user action and are not bundled with the application.

The repository currently defines two independent catalogs:

- `wallpapers.json` for optional background images;
- `fonts.json` for optional fonts (the catalog is reserved for future use).

The catalogs are empty until the first reviewed add-on is added. Users may also select their own local wallpaper without downloading anything from this repository.

Catalog URL:

```text
https://raw.githubusercontent.com/andrewpozdnakov7-jpg/Slooop_Addons/main/wallpapers.json
```

## Adding an add-on

Every entry must provide its author, license, dimensions, file size and SHA-256 hash. Only images that can be redistributed under the declared license may be added. Do not commit source prompts, personal metadata or unreviewed third-party images.

Store wallpaper previews in `wallpapers/previews/`, wallpaper files in `wallpapers/images/`, and future fonts in `fonts/files/`. Use stable lowercase identifiers and keep existing files immutable after publication; replace an asset with a new identifier instead.

The wallpaper catalog format is documented and validated by `wallpapers.schema.json`.

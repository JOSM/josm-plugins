README 
======

The FIT plugin is written for Garmin FIT files.

The `org.openstreetmap.josm.plugins.fit.lib` package does not have any
dependency on JOSM; you should be able to import that into your code with
no issues.

## License
This plugin is licensed under the GPLv2 or later.

## Usage
The main entry point for the library is `FitReader.read(InputStream inputStream)`.
You _should not_ use any of the `public` constructors; they may change at any time.
You also _should not_ use any of the `static <return> parse` methods.

## Authors
Taylor Smock <taylor.smock@kaart.com>

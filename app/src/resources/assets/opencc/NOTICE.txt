# Offline traditional conversion data

These five files are copied byte-for-byte from `crates/client-core/data/opencc/` in the bundled original MSIME source archive, android-v0.3.1, commit b5c1b46ad049480420c9501e3bd85ac7fbc2ed26. They retain their OpenCC license in LICENSE. The host implements s2t normalization and phrase-first maximum matching; no network or platform transliteration service is used.

The native binaries, dictionaries and their notices remain unchanged. Chinese candidate labels and committed Chinese text use the same converter; typed Code/English, clipboard text and emoji are not converted.

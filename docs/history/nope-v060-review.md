# NOPE 0.6.0 delivery review

## Implementation evidence

- Initial editor selection is recorded as an expected asynchronous echo. Receiving it after the first composed letter no longer cancels new composition. Regression checks also retain detection of a real external cursor move.
- Tap jitter is independent of deliberate variant/editing gestures. Key-area motion splitting is enabled, and an unrelated popup no longer invalidates an ordinary tap. Current-session input is buffered while the Chinese engine or conversion table prepares.
- Long-press selection uses the popup's physical screen origin, including edge clipping. The earlier virtual origin could make left items unreachable; up-swipe cancellation is retained.
- Emoji cells use dedicated Canvas rendering and fit both dimensions. Missing/blank glyphs are filtered against the current system font. Common emoji are ranked by local count, then recent use; private fields do not record use.
- Skin-specific key, function and selected roles share palette functions in rendering and contrast tests. The independent finish review identified an invisible unselected Shift in light color; that issue was corrected and explicitly re-reviewed as resolved.

These identify and test code paths; no recording of the reported bugs running in NOPE was available. The supplied video depicts a skin-store showcase.

## Completed local verification

`scripts/test_local.py` passed the original input/privacy/editor suites, NOPE input and retention/conversion/skin checks, and the Unicode 17 catalog/license check. The final Gradle build completed `assembleDebug` and `lintDebug`: zero errors, twenty known warnings. APK verification checks VIBRATE as the only requested permission, ARM64 native dependency closure, unchanged original dictionary/library hashes, Unicode/OpenCC data and licenses, NOPE label, signature and 16KB alignment. Source delivery checks the final files and excludes build caches and signing keys.

The finish review's final disposition is **ship**, restricted to the listed repair and development APK delivery. Motion review is **Approve** at code level; neither grants native visual or device acceptance.

## Device acceptance

ADB reported no attached devices. Still verify rapid alternating/two-finger typing after opening a field, candidate-to-letter transitions, held left-edge variants, emoji glyphs on the target phone, light color Shift on/off, all skin materials, host editor operations and vibration strength. Fixed screenshot-inspired skins retain their supplied appearance; system, color and optional iOS follow night mode. The preview is generated from actual palette values but remains a schematic.

Clipboard retention concerns this input method's history and hint, not the operating system clipboard. Pinned items remain retained; process downtime is cleared on the next start. The stable application ID and development certificate support updating earlier local APKs without intentionally resetting stored preferences.

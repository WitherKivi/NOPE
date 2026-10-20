# 0.6.0 motion review

| Before | After | Why |
| --- | --- | --- |
| Rapid taps could be cancelled after 9dp of incidental movement. | Ordinary taps allow 18dp jitter; intentional variants/editing gestures have separate arming thresholds. | Frequent input must respond immediately and distinguish a gesture from incidental finger motion. See `InputTouchPolicy.java:6` and `OfflineImeService.java:491`. |
| A popup shifted against the screen edge, while selection followed a virtual origin. | Selection follows the popup's actual screen coordinates. | Visual and physical selection origins now match; left choices remain reachable. See `OfflineImeService.java:439–442`. |

**Approve — code-level motion review.** Key presses have immediate state feedback and one configured vibration pulse, without an elevation animator (`KeyButton.java:26`, `OfflineImeService.java:513–514`). Variant and editing popups use animation style zero (`OfflineImeService.java:431`, `:476`); interruption, cancellation and editor generation checks happen before committing. Tools replace the keyboard body. No timed movement was added to these high-frequency interactions, so no custom reduced-motion branch is required. Native scrolling retains platform gesture behavior.

This verdict does not assert perceived latency, frame pacing or tactile quality on a phone. No Android device was connected. Touch timing, popup placement, scroll handoff and vibration remain device acceptance items. The skin preview is a static Pillow schematic, not runtime evidence.

References above are relative to `app/src/main/java/app/msime/offline/`. Native Java behavior is evaluated against the frequency, physical-origin and interruptibility principles in the animate, emil-design-eng, apple-design and review-animations skills.

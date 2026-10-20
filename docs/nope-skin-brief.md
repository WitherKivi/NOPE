# NOPE 0.6 skin and input upgrade

## Direction contract

THESIS: An offline daily keyboard and terminal keyboard whose shared layout accepts the user's named skin families. The user supplied exact references; there is no open concept selection. Native key hit testing and reliable composition lead.

OWN-WORLD: Default system light/dark raised neutral caps with restrained pink/blue sheen; cloudy white soft caps; black keys on gray with red accent; brushed metal/navy caps/cyan accent; white double black outlines; connected black row tracks with hot-pink accents and dual appearance. N replaces third-party branding. iOS remains optional.

STORY: Type without losing the first letter, choose variants by physical popup position, select horizontally separated candidates, access frequent emoji, control clipboard retention and change skins locally.

FIRST VIEWPORT: Normal keyboard shares a 48dp toolbar/candidate row, three letter rows and a blank-space bottom row. Period is primary, comma is secondary. Bold accent-filled 中/EN shows current mode. Tools and skins replace keyboard body. Code retains control and navigation rows and shows suggestions only while typing.

FORM: User-selected screenshot families, adapting materials to the existing Gboard-like layout. No copied raster keyboard, font, du mark or network feature. The context launcher failed; project context was read directly. Authoring and preview are code-led.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.

## Evidence and scope

`nope-skins-preview.png` is a palette/layout schematic produced by `scripts/render_skins.py` from actual Java tokens. It is not a native Android screenshot. The supplied video is a skin-store preview, not a NOPE bug reproduction. ADB reports no devices, and no emulator is installed. The finish review can assess implementation, tokens, tests and package readiness; live rendering, two-finger timing, host editor behavior, unsupported emoji fonts and tactile feedback remain device acceptance items. Do not give full native visual approval from the schematic.

## Original requested behavior

Fix rapid missing first letter, long-press left selection and emoji empty cells; primary period, frequency-ranked common emoji, candidate dividers, visible Chinese/English status; custom clipboard retention or never clear; in-keyboard tools and traditional toggle; all named screenshot skins; rename to NOPE 输入法 with N app icon. Preserve offline ARM64 engine, privacy policies, adjustable haptics and heights, editor gestures, Code no toolbar, completion fill-only and full ASCII reachability.

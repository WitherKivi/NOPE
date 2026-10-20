package app.msime.offline;

import android.content.Context;

/** Use native text rendering so direction controls never rely on an empty custom-drawn Button. */
final class ArrowButton extends KeyButton {
    ArrowButton(Context context, int direction) { super(context); setText(new String[]{"←", "↑", "→", "↓"}[direction]); }
}

package app.msime.offline;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Only touch feedback; hardware/system restrictions may suppress or limit amplitude. */
final class KeyFeedback {
    private final SharedPreferences preferences;
    private final Vibrator vibrator;
    private final AudioAttributes attributes = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
    KeyFeedback(Context context) {
        preferences = context.getSharedPreferences("offline", Context.MODE_PRIVATE);
        vibrator = context.getSystemService(Vibrator.class);
    }
    void pulse() {
        if (!preferences.getBoolean("haptics", true)) return;
        int strength = HapticStrength.percent(preferences.getInt("haptic_strength", 35));
        if (strength == 0 || vibrator == null) return;
        try {
            if (!vibrator.hasVibrator()) return;
            int amplitude = vibrator.hasAmplitudeControl() ? HapticStrength.amplitude(strength) : VibrationEffect.DEFAULT_AMPLITUDE;
            vibrator.vibrate(VibrationEffect.createOneShot(HapticStrength.duration(strength), amplitude), attributes);
        } catch (RuntimeException ignored) { /* Feedback must never interrupt typing. */ }
    }
}

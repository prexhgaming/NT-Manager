package bin.nt.plus;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

/**
 * Launcher screen. Kept deliberately small: it is the starting point for the real UI.
 */
public class MainActivity extends AppCompatActivity {

    private static final String KEY_TAPS = "taps";

    private int tapCount;
    private TextView counterView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            tapCount = savedInstanceState.getInt(KEY_TAPS, 0);
        }

        counterView = findViewById(R.id.text_counter);
        MaterialButton actionButton = findViewById(R.id.button_action);

        applyWindowInsets(findViewById(R.id.root));
        renderCounter();

        actionButton.setOnClickListener(view -> {
            tapCount++;
            renderCounter();
            Toast.makeText(this, getString(R.string.tap_message, tapCount), Toast.LENGTH_SHORT).show();
        });
    }

    /** The app targets SDK 37, so the system draws edge to edge - keep content out of the bars. */
    private void applyWindowInsets(View root) {
        final int basePaddingLeft = root.getPaddingLeft();
        final int basePaddingTop = root.getPaddingTop();
        final int basePaddingRight = root.getPaddingRight();
        final int basePaddingBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    basePaddingLeft + bars.left,
                    basePaddingTop + bars.top,
                    basePaddingRight + bars.right,
                    basePaddingBottom + bars.bottom);
            return windowInsets;
        });
    }

    private void renderCounter() {
        counterView.setText(getString(R.string.counter_value, tapCount));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAPS, tapCount);
    }
}

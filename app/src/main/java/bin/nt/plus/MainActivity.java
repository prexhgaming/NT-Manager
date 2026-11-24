package bin.nt.plus;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

import bin.nt.plus.ui.FileBrowserFragment;
import bin.nt.plus.ui.FilePanelView;

/**
 * Hosts the dual pane file manager: app bar on top, the two panes in a fragment,
 * and the five primary actions in the bottom navigation bar.
 */
public class MainActivity extends AppCompatActivity implements FileBrowserFragment.Host {

    private static final String TAG_BROWSER = "file_browser";

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomActions;
    private FileBrowserFragment browser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        bottomActions = findViewById(R.id.bottom_actions);

        applyWindowInsets(findViewById(R.id.root));

        FragmentManager fragments = getSupportFragmentManager();
        browser = (FileBrowserFragment) fragments.findFragmentByTag(TAG_BROWSER);
        if (browser == null) {
            browser = new FileBrowserFragment();
            fragments.beginTransaction()
                    .replace(R.id.panes_container, browser, TAG_BROWSER)
                    .commitNow();
        }

        bottomActions.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_back) {
                browser.goBack();
                return true;
            }
            if (id == R.id.action_forward) {
                browser.goForward();
                return true;
            }
            if (id == R.id.action_up) {
                browser.goUp();
                return true;
            }
            if (id == R.id.action_new_folder) {
                promptNewFolder();
                return true;
            }
            if (id == R.id.action_refresh) {
                browser.refresh();
                return true;
            }
            return false;
        });

        updateAppBar(browser.getActivePanel());
        ensureStorageAccess();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Panes may have been blocked by the permission dialog - re-read once we are back.
        if (browser != null && hasStorageAccess()) {
            browser.refresh();
        }
    }

    private boolean hasStorageAccess() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    /** Without all-files access the panes cannot list internal storage, so ask once. */
    private void ensureStorageAccess() {
        if (hasStorageAccess()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.storage_access_required)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.grant_access, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        return;
                    }
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException unavailable) {
                        Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
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

    // ------------------------------------------------------------------------------------
    // FileBrowserFragment.Host
    // ------------------------------------------------------------------------------------

    @Override
    public void onActivePanelChanged(@NonNull FilePanelView panel) {
        updateAppBar(panel);
    }

    /** App bar keeps the app name as title and mirrors the active pane in the subtitle. */
    private void updateAppBar(@NonNull FilePanelView panel) {
        toolbar.setTitle(R.string.app_name);
        toolbar.setSubtitle(buildSummary(panel, panel.getDirectory()));
    }

    private String buildSummary(@NonNull FilePanelView panel, @NonNull File directory) {
        String disk = getString(R.string.not_available);
        try {
            StatFs statFs = new StatFs(directory.getAbsolutePath());
            long free = statFs.getAvailableBytes();
            long total = statFs.getTotalBytes();
            disk = Formatter.formatShortFileSize(this, total - free)
                    + "/" + Formatter.formatShortFileSize(this, total);
        } catch (IllegalArgumentException ignored) {
            // Path is not backed by a readable filesystem - keep the placeholder.
        }
        return getString(R.string.panel_summary, panel.getFolderCount(), panel.getFileCount(), disk);
    }

    // ------------------------------------------------------------------------------------
    // New folder
    // ------------------------------------------------------------------------------------

    private void promptNewFolder() {
        EditText input = new EditText(this);
        input.setHint(R.string.new_folder_hint);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.new_folder_title)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.create, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.invalid_name, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    File created = browser.createFolder(name);
                    if (created != null) {
                        Toast.makeText(this, getString(R.string.folder_created, created.getName()),
                                Toast.LENGTH_SHORT).show();
                        browser.refresh();
                    } else {
                        Toast.makeText(this, getString(R.string.folder_create_failed, name),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}

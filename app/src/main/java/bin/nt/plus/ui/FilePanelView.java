package bin.nt.plus.ui;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import bin.nt.plus.R;
import bin.nt.plus.fs.FileAdapter;
import bin.nt.plus.fs.FileEntry;

/**
 * A self contained file browser panel. The dual pane screen simply instantiates two of these
 * and drives both through the same public API:
 *
 * <pre>
 *   panel.setDirectory(dir)   panel.goBack()   panel.goForward()
 *   panel.goUp()              panel.refresh()  panel.createFolder(name)
 * </pre>
 *
 * Every panel keeps its own directory, history stack and selection, so the two panes never
 * interfere with each other.
 */
public class FilePanelView extends FrameLayout {

    /** Notified whenever the panel's directory or contents change. */
    public interface Listener {
        void onPanelChanged(@NonNull FilePanelView panel);

        void onPanelActivated(@NonNull FilePanelView panel);
    }

    private final RecyclerView list;
    private final TextView emptyView;
    private final FileAdapter adapter;

    private final Deque<File> backStack = new ArrayDeque<>();
    private final Deque<File> forwardStack = new ArrayDeque<>();

    private File directory;
    private FileEntry selected;
    private Listener listener;

    public FilePanelView(@NonNull Context context) {
        this(context, null);
    }

    public FilePanelView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FilePanelView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_file_panel, this, true);

        list = findViewById(R.id.panel_list);
        emptyView = findViewById(R.id.panel_empty);
        list.setLayoutManager(new LinearLayoutManager(context));

        adapter = new FileAdapter(this::onEntryClicked);
        list.setAdapter(adapter);

        setBackgroundResource(R.color.pane_inactive_background);
        directory = defaultDirectory();
        reload();
    }

    // ------------------------------------------------------------------------------------
    // Public API - identical for both panels
    // ------------------------------------------------------------------------------------

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @NonNull
    public File getDirectory() {
        return directory;
    }

    @Nullable
    public FileEntry getSelected() {
        return selected;
    }

    public boolean canGoBack() {
        return !backStack.isEmpty();
    }

    public boolean canGoForward() {
        return !forwardStack.isEmpty();
    }

    public boolean canGoUp() {
        return directory.getParentFile() != null;
    }

    /** Opens a directory, pushing the current one onto the back stack. */
    public void setDirectory(@NonNull File target) {
        File resolved = target;
        if (!resolved.isDirectory() && resolved.getParentFile() != null) {
            resolved = resolved.getParentFile();
        }
        if (resolved.equals(directory)) {
            refresh();
            return;
        }
        backStack.push(directory);
        forwardStack.clear();
        directory = resolved;
        selected = null;
        reload();
    }

    public void goBack() {
        if (backStack.isEmpty()) {
            return;
        }
        forwardStack.push(directory);
        directory = backStack.pop();
        selected = null;
        reload();
    }

    public void goForward() {
        if (forwardStack.isEmpty()) {
            return;
        }
        backStack.push(directory);
        directory = forwardStack.pop();
        selected = null;
        reload();
    }

    public void goUp() {
        File parent = directory.getParentFile();
        if (parent != null) {
            setDirectory(parent);
        }
    }

    public void refresh() {
        reload();
    }

    /** Creates a folder in the current directory and opens nothing. Returns the new dir or null. */
    @Nullable
    public File createFolder(@NonNull String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        File target = new File(directory, trimmed);
        if (target.exists()) {
            return null;
        }
        return target.mkdirs() ? target : null;
    }

    /** Number of directories currently listed (excluding the ".." parent row). */
    public int getFolderCount() {
        int count = 0;
        for (FileEntry entry : currentEntries()) {
            if (entry.isDirectory() && !entry.isParentLink()) {
                count++;
            }
        }
        return count;
    }

    /** Number of plain files currently listed. */
    public int getFileCount() {
        int count = 0;
        for (FileEntry entry : currentEntries()) {
            if (!entry.isDirectory()) {
                count++;
            }
        }
        return count;
    }

    public void setActive(boolean active) {
        setBackgroundResource(active ? R.color.pane_active_background : R.color.pane_inactive_background);
    }

    // ------------------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------------------

    private void onEntryClicked(FileEntry entry) {
        if (listener != null) {
            listener.onPanelActivated(this);
        }
        if (entry.isDirectory()) {
            setDirectory(entry.getFile());
            return;
        }
        selected = entry;
        adapter.notifyDataSetChanged();
    }

    private void reload() {
        List<FileEntry> entries = currentEntries();
        adapter.submit(entries);
        emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        emptyView.setText(R.string.empty_folder);
        if (listener != null) {
            listener.onPanelChanged(this);
        }
    }

    @NonNull
    private List<FileEntry> currentEntries() {
        List<FileEntry> result = new ArrayList<>();
        File parent = directory.getParentFile();
        if (parent != null) {
            result.add(FileEntry.parent(parent, getContext().getString(R.string.parent_directory)));
        }

        File[] children;
        try {
            children = directory.listFiles();
        } catch (SecurityException denied) {
            children = null;
        }
        if (children == null) {
            return result;
        }

        List<FileEntry> dirs = new ArrayList<>();
        List<FileEntry> files = new ArrayList<>();
        for (File child : children) {
            if (child.isDirectory()) {
                dirs.add(FileEntry.of(child));
            } else {
                files.add(FileEntry.of(child));
            }
        }
        Collections.sort(dirs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        Collections.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        result.addAll(dirs);
        result.addAll(files);
        return result;
    }

    @NonNull
    private static File defaultDirectory() {
        File external = Environment.getExternalStorageDirectory();
        if (external != null && external.isDirectory()) {
            return external;
        }
        return Environment.getRootDirectory();
    }
}

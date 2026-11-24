package bin.nt.plus.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;

import bin.nt.plus.R;

/**
 * Hosts the dual panel file manager. Both panes are the same {@link FilePanelView} class,
 * created as two independent instances; the fragment owns the "active pane" concept and
 * forwards the app bar / bottom navigation actions to the right instance.
 */
public class FileBrowserFragment extends Fragment implements FilePanelView.Listener {

    /** Implemented by the activity so the toolbar can follow the active pane. */
    public interface Host {
        void onActivePanelChanged(@NonNull FilePanelView panel);
    }

    private FilePanelView leftPanel;
    private FilePanelView rightPanel;
    private FilePanelView activePanel;

    public FileBrowserFragment() {
        super(R.layout.fragment_file_browser);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        leftPanel = view.findViewById(R.id.panel_left);
        rightPanel = view.findViewById(R.id.panel_right);

        leftPanel.setListener(this);
        rightPanel.setListener(this);

        activePanel = leftPanel;
        leftPanel.setActive(true);
        rightPanel.setActive(false);

        notifyHost();
    }

    // ------------------------------------------------------------------------------------
    // Actions forwarded from the app bar / bottom navigation
    // ------------------------------------------------------------------------------------

    @NonNull
    public FilePanelView getActivePanel() {
        return activePanel;
    }

    public void goBack() {
        activePanel.goBack();
    }

    public void goForward() {
        activePanel.goForward();
    }

    public void goUp() {
        activePanel.goUp();
    }

    public void refresh() {
        activePanel.refresh();
    }

    /** Creates a folder in the active panel. Returns the created dir, or null on failure. */
    @Nullable
    public File createFolder(@NonNull String name) {
        return activePanel.createFolder(name);
    }

    public void activate(@NonNull FilePanelView panel) {
        if (panel == activePanel) {
            return;
        }
        activePanel.setActive(false);
        activePanel = panel;
        activePanel.setActive(true);
        notifyHost();
    }

    // ------------------------------------------------------------------------------------
    // FilePanelView.Listener
    // ------------------------------------------------------------------------------------

    @Override
    public void onPanelChanged(@NonNull FilePanelView panel) {
        if (panel == activePanel) {
            notifyHost();
        }
    }

    @Override
    public void onPanelActivated(@NonNull FilePanelView panel) {
        activate(panel);
    }

    private void notifyHost() {
        if (getActivity() instanceof Host) {
            ((Host) getActivity()).onActivePanelChanged(activePanel);
        }
    }
}

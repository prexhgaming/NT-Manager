package bin.nt.plus.fs;

import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import bin.nt.plus.R;

/**
 * Renders the rows of a single file panel. Stateless apart from the list it is given,
 * so each FilePanelView owns its own instance.
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.RowHolder> {

    public interface OnEntryClickListener {
        void onEntryClick(FileEntry entry);
    }

    private final List<FileEntry> entries = new ArrayList<>();
    private final OnEntryClickListener clickListener;

    public FileAdapter(OnEntryClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submit(List<FileEntry> newEntries) {
        entries.clear();
        if (newEntries != null) {
            entries.addAll(newEntries);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new RowHolder(row, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static final class RowHolder extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView name;
        private final TextView meta;
        private final OnEntryClickListener clickListener;

        RowHolder(@NonNull View itemView, OnEntryClickListener clickListener) {
            super(itemView);
            this.clickListener = clickListener;
            icon = itemView.findViewById(R.id.item_icon);
            name = itemView.findViewById(R.id.item_name);
            meta = itemView.findViewById(R.id.item_meta);
            itemView.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || clickListener == null) {
                    return;
                }
                FileAdapter adapter = (FileAdapter) getBindingAdapter();
                if (adapter == null || position >= adapter.entries.size()) {
                    return;
                }
                clickListener.onEntryClick(adapter.entries.get(position));
            });
        }

        void bind(FileEntry entry) {
            name.setText(entry.getName());
            icon.setImageResource(entry.isDirectory() ? R.drawable.ic_folder : R.drawable.ic_file);

            if (entry.isDirectory()) {
                meta.setVisibility(View.GONE);
            } else {
                meta.setVisibility(View.VISIBLE);
                meta.setText(Formatter.formatShortFileSize(itemView.getContext(), entry.getSize()));
            }
        }
    }
}

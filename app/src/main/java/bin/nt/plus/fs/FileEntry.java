package bin.nt.plus.fs;

import java.io.File;

/**
 * One row in a file panel: either a real file/directory or the synthetic ".." parent entry.
 */
public final class FileEntry {

    private final File file;
    private final String name;
    private final boolean directory;
    private final boolean parentLink;
    private final long size;
    private final long lastModified;

    private FileEntry(File file, String name, boolean directory, boolean parentLink,
                      long size, long lastModified) {
        this.file = file;
        this.name = name;
        this.directory = directory;
        this.parentLink = parentLink;
        this.size = size;
        this.lastModified = lastModified;
    }

    public static FileEntry of(File file) {
        return new FileEntry(file, file.getName(), file.isDirectory(), false,
                file.length(), file.lastModified());
    }

    public static FileEntry parent(File parentDir, String label) {
        return new FileEntry(parentDir, label, true, true, 0L, parentDir.lastModified());
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return directory;
    }

    public boolean isParentLink() {
        return parentLink;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }
}

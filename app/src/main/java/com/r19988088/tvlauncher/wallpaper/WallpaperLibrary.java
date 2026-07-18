package com.r19988088.tvlauncher.wallpaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WallpaperLibrary {
    private final File root;
    private final File savedDirectory;

    public WallpaperLibrary(File root) {
        this.root = root;
        savedDirectory = new File(root, "saved");
    }

    public File currentFile() {
        return new File(root, "random-wallpaper.image");
    }

    public File saveCurrent() throws IOException {
        File current = currentFile();
        if (!current.isFile()) throw new IOException("No downloaded wallpaper");
        if (!savedDirectory.isDirectory() && !savedDirectory.mkdirs()) {
            throw new IOException("Cannot create wallpaper library");
        }
        File target = new File(savedDirectory,
                "wallpaper-" + current.lastModified() + '-' + current.length() + ".image");
        if (target.isFile()) return target;
        File temporary = new File(savedDirectory, target.getName() + ".tmp");
        try (FileInputStream input = new FileInputStream(current);
                FileOutputStream output = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        } catch (IOException failure) {
            temporary.delete();
            throw failure;
        }
        if (!temporary.renameTo(target)) {
            temporary.delete();
            throw new IOException("Cannot save wallpaper");
        }
        return target;
    }

    public List<File> list() {
        File[] files = savedDirectory.listFiles(file ->
                file.isFile() && file.getName().endsWith(".image"));
        if (files == null) return new ArrayList<>();
        Arrays.sort(files, (left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        return new ArrayList<>(Arrays.asList(files));
    }

    public boolean delete(File file) {
        try {
            File candidate = file.getCanonicalFile();
            if (!savedDirectory.getCanonicalFile().equals(candidate.getParentFile())) return false;
            return candidate.isFile() && candidate.delete();
        } catch (IOException failure) {
            return false;
        }
    }
}

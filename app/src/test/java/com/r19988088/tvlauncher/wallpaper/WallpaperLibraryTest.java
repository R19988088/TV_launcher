package com.r19988088.tvlauncher.wallpaper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class WallpaperLibraryTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void onlySavedDownloadsAppearInLocalLibrary() throws Exception {
        File root = temporaryFolder.newFolder("wallpapers");
        File current = new File(root, "random-wallpaper.image");
        try (FileOutputStream output = new FileOutputStream(current)) {
            output.write("image".getBytes(StandardCharsets.UTF_8));
        }
        WallpaperLibrary library = new WallpaperLibrary(root);

        assertEquals(0, library.list().size());
        File saved = library.saveCurrent();
        List<File> local = library.list();

        assertEquals(1, local.size());
        assertEquals(saved, local.get(0));
        assertEquals(saved, library.saveCurrent());
        assertEquals(1, library.list().size());
    }

    @Test
    public void deletesOnlyFilesFromSavedLibrary() throws Exception {
        File root = temporaryFolder.newFolder("delete-wallpapers");
        File current = new File(root, "random-wallpaper.image");
        try (FileOutputStream output = new FileOutputStream(current)) {
            output.write("image".getBytes(StandardCharsets.UTF_8));
        }
        WallpaperLibrary library = new WallpaperLibrary(root);
        File saved = library.saveCurrent();

        assertTrue(library.delete(saved));
        assertFalse(saved.exists());
        assertFalse(library.delete(current));
        assertTrue(current.exists());
    }
}

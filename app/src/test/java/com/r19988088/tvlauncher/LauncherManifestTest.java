package com.r19988088.tvlauncher;

import static org.junit.Assert.assertTrue;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class LauncherManifestTest {
    private static final String VENDOR_SYSTEM_UI_PERMISSION =
            "com.xiaomi.mitv.systemui.MiTVSystemUIService.SEND_COMMAND";

    @Test
    public void requestsVendorSystemUiCommandPermission() throws Exception {
        Element manifest = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new File("src/main/AndroidManifest.xml"))
                .getDocumentElement();

        assertTrue("TV Launcher must be allowed to hide the vendor status bar",
                hasPermission(manifest, VENDOR_SYSTEM_UI_PERMISSION));
    }

    @Test
    public void homeAndLeanbackEntriesUseSeparateIntentFilters() throws Exception {
        File manifest = new File("src/main/AndroidManifest.xml");
        Element activity = (Element) DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(manifest)
                .getElementsByTagName("activity")
                .item(0);

        boolean hasHome = false;
        boolean hasLeanback = false;
        NodeList filters = activity.getElementsByTagName("intent-filter");
        for (int index = 0; index < filters.getLength(); index++) {
            Element filter = (Element) filters.item(index);
            boolean home = hasCategory(filter, "android.intent.category.HOME");
            boolean leanback = hasCategory(filter, "android.intent.category.LEANBACK_LAUNCHER");
            assertTrue("HOME and LEANBACK_LAUNCHER must not share an intent-filter",
                    !home || !leanback);
            hasHome |= home && hasCategory(filter, "android.intent.category.DEFAULT");
            hasLeanback |= leanback;
        }

        assertTrue("LauncherActivity must expose MAIN + HOME + DEFAULT", hasHome);
        assertTrue("LauncherActivity must remain visible to Android TV", hasLeanback);
    }

    private static boolean hasCategory(Element filter, String categoryName) {
        NodeList categories = filter.getElementsByTagName("category");
        for (int index = 0; index < categories.getLength(); index++) {
            Element category = (Element) categories.item(index);
            if (categoryName.equals(category.getAttribute("android:name"))) return true;
        }
        return false;
    }

    private static boolean hasPermission(Element manifest, String permissionName) {
        NodeList permissions = manifest.getElementsByTagName("uses-permission");
        for (int index = 0; index < permissions.getLength(); index++) {
            Element permission = (Element) permissions.item(index);
            if (permissionName.equals(permission.getAttribute("android:name"))) return true;
        }
        return false;
    }
}

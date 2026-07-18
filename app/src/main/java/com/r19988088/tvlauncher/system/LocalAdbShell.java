package com.r19988088.tvlauncher.system;

import android.util.Base64;
import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

final class LocalAdbShell {
    private static final int ADB_PORT = 5555;
    private final File privateKey;
    private final File publicKey;

    LocalAdbShell(File dataDirectory) {
        privateKey = new File(dataDirectory, "local-adb.key");
        publicKey = new File(dataDirectory, "local-adb.key.pub");
    }

    void execute(String command) throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", ADB_PORT), 1500);
        socket.setSoTimeout(15000);
        try (AdbConnection connection = AdbConnection.create(socket, loadOrCreateKey())) {
            connection.connect();
            try (AdbStream stream = connection.open("shell:" + command)) {
                drain(stream);
            }
        }
    }

    private AdbCrypto loadOrCreateKey() throws Exception {
        if (privateKey.isFile() && publicKey.isFile()) {
            try {
                return AdbCrypto.loadAdbKeyPair(
                        data -> Base64.encodeToString(data, Base64.NO_WRAP),
                        privateKey,
                        publicKey);
            } catch (Exception invalidKey) {
                privateKey.delete();
                publicKey.delete();
            }
        }
        AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(
                data -> Base64.encodeToString(data, Base64.NO_WRAP));
        crypto.saveAdbKeyPair(privateKey, publicKey);
        return crypto;
    }

    private static void drain(AdbStream stream) throws InterruptedException {
        try {
            while (true) stream.read();
        } catch (IOException commandFinished) {
            // The legacy ADB shell protocol signals command completion by closing the stream.
        }
    }
}

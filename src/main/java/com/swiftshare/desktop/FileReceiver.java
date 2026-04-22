package com.swiftshare.desktop;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileReceiver {
    private static final int PORT = 8988;
    private static final int CHUNK_SIZE = 64 * 1024; // 64 KB
    private static final String METADATA_DELIMITER = "|";

    private final JTextArea logArea;
    private final JLabel statusLabel;
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread serverThread;

    public FileReceiver(JTextArea logArea, JLabel statusLabel) {
        this.logArea = logArea;
        this.statusLabel = statusLabel;
    }

    public void start() {
        if (running.get()) return;
        running.set(true);
        serverThread = new Thread(this::runServer);
        serverThread.start();
    }

    public void stop() {
        running.set(false);
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    private void runServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            while (running.get()) {
                Socket client = serverSocket.accept();
                handleClient(client);
            }
        } catch (IOException e) {
            if (running.get()) {
                log("Server error: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket client) {
        try {
            DataInputStream input = new DataInputStream(client.getInputStream());
            DataOutputStream output = new DataOutputStream(client.getOutputStream());

            // Read metadata
            String metadata = input.readUTF();
            String[] parts = metadata.split("\\" + METADATA_DELIMITER);
            String fileName = parts[0];
            long fileSize = Long.parseLong(parts[1]);
            String expectedMd5 = parts.length > 2 ? parts[2] : "";

            log("Receiving: " + fileName + " (" + formatSize(fileSize) + ")");
            updateStatus("Receiving " + fileName);

            // Save to user's home/SwiftShare folder
            File saveDir = new File(System.getProperty("user.home"), "SwiftShare");
            if (!saveDir.exists()) saveDir.mkdirs();
            File outputFile = new File(saveDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[CHUNK_SIZE];
                long totalRead = 0;
                int bytesRead;
                long lastLogTime = System.currentTimeMillis();

                while (totalRead < fileSize) {
                    bytesRead = input.read(buffer, 0, (int) Math.min(CHUNK_SIZE, fileSize - totalRead));
                    if (bytesRead == -1) break;
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;

                    // Log progress every second
                    long now = System.currentTimeMillis();
                    if (now - lastLogTime > 1000) {
                        int percent = (int) (totalRead * 100 / fileSize);
                        updateStatus("Receiving " + fileName + " (" + percent + "%)");
                        lastLogTime = now;
                    }
                }
            }

            // Verify MD5 if provided
            if (!expectedMd5.isEmpty()) {
                String actualMd5 = calculateMd5(outputFile);
                if (actualMd5.equals(expectedMd5)) {
                    output.writeUTF("OK");
                    log("Received successfully: " + fileName);
                } else {
                    output.writeUTF("ERROR:MD5_MISMATCH");
                    log("MD5 mismatch for: " + fileName);
                    outputFile.delete();
                }
            } else {
                output.writeUTF("OK");
                log("Received: " + fileName);
            }

            updateStatus("Ready");
            client.close();
        } catch (Exception e) {
            log("Error receiving file: " + e.getMessage());
        }
    }

    private String calculateMd5(File file) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }

    public String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // Ignore
        }
        return "Unknown";
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }
}

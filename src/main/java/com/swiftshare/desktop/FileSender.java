package com.swiftshare.desktop;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;

public class FileSender {
    private static final int CHUNK_SIZE = 64 * 1024;
    private static final String METADATA_DELIMITER = "|";
    
    private final JTextArea logArea;
    private final JLabel statusLabel;
    
    public FileSender(JTextArea logArea, JLabel statusLabel) {
        this.logArea = logArea;
        this.statusLabel = statusLabel;
    }
    
    public void sendFile(File file, String targetIp, int port) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(targetIp, port);
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                DataInputStream input = new DataInputStream(socket.getInputStream());
                
                String fileName = file.getName();
                long fileSize = file.length();
                
                log("Sending: " + fileName + " (" + formatSize(fileSize) + ")");
                updateStatus("Sending " + fileName);
                
                // Calculate MD5
                String md5 = calculateMd5(file);
                
                // Send metadata
                String metadata = fileName + METADATA_DELIMITER + fileSize + METADATA_DELIMITER + md5;
                output.writeUTF(metadata);
                
                // Send file data
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[CHUNK_SIZE];
                    int bytesRead;
                    long totalSent = 0;
                    long lastLogTime = System.currentTimeMillis();
                    
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                        totalSent += bytesRead;
                        
                        long now = System.currentTimeMillis();
                        if (now - lastLogTime > 1000) {
                            int percent = (int) (totalSent * 100 / fileSize);
                            updateStatus("Sending " + fileName + " (" + percent + "%)");
                            lastLogTime = now;
                        }
                    }
                }
                
                // Wait for confirmation
                String response = input.readUTF();
                if ("OK".equals(response)) {
                    log("Sent successfully: " + fileName);
                    updateStatus("Ready");
                } else {
                    log("Send failed: " + response);
                    updateStatus("Send failed");
                }
                
                socket.close();
            } catch (Exception e) {
                log("Error sending file: " + e.getMessage());
                updateStatus("Error");
            }
        }).start();
    }
    
    private String calculateMd5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
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
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Sender: " + text));
    }
}

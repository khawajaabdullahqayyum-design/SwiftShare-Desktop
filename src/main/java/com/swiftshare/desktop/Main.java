package com.swiftshare.desktop;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Main {
    private static JTextArea logArea;
    private static JLabel statusLabel;
    private static FileReceiver receiver;
    private static JButton startButton;
    private static JButton stopButton;
    private static JButton openFolderButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("SwiftShare Desktop Receiver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout(10, 10));

        // Top panel with controls
        JPanel topPanel = new JPanel(new FlowLayout());
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);
        openFolderButton = new JButton("Open Received Files");
        
        statusLabel = new JLabel("Server not running");
        statusLabel.setForeground(Color.RED);

        topPanel.add(startButton);
        topPanel.add(stopButton);
        topPanel.add(openFolderButton);
        topPanel.add(statusLabel);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Button actions
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        openFolderButton.addActionListener(e -> openReceivedFolder());

        frame.setVisible(true);
    }

    private static void startServer() {
        receiver = new FileReceiver(logArea, statusLabel);
        receiver.start();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("Server running on port 8988");
        statusLabel.setForeground(new Color(0, 128, 0));
        log("Server started. Waiting for connections...");
        log("Your IP: " + receiver.getLocalIpAddress());
    }

    private static void stopServer() {
        if (receiver != null) {
            receiver.stop();
            receiver = null;
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("Server stopped");
        statusLabel.setForeground(Color.RED);
        log("Server stopped.");
    }

    private static void openReceivedFolder() {
        try {
            File folder = new File(System.getProperty("user.home"), "SwiftShare");
            if (!folder.exists()) folder.mkdirs();
            Desktop.getDesktop().open(folder);
        } catch (Exception e) {
            log("Error opening folder: " + e.getMessage());
        }
    }

    public static void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}

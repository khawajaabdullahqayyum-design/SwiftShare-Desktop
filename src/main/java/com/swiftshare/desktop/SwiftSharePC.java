package com.swiftshare.desktop;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class SwiftSharePC extends JFrame {
    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel ipLabel;
    private JTextField targetIpField;
    private JButton startServerButton;
    private JButton stopServerButton;
    private JButton sendFileButton;
    private JButton showQRButton;
    private JButton openFolderButton;
    
    private FileReceiver receiver;
    private FileSender sender;
    private QRGenerator qrGenerator;
    
    private static final int PORT = 8988;

    public SwiftSharePC() {
        setTitle("SwiftShare PC - Send & Receive Files");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLayout(new BorderLayout(10, 10));
        
        qrGenerator = new QRGenerator();
        
        createUI();
        
        // Display local IP
        String localIp = getLocalIpAddress();
        ipLabel.setText("Your IP: " + localIp + ":" + PORT);
        
        setVisible(true);
    }
    
    private void createUI() {
        // Top Panel
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // IP Display
        ipLabel = new JLabel("Your IP: Detecting...");
        ipLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(ipLabel);
        
        // Connection Panel
        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionPanel.add(new JLabel("Connect to IP:"));
        targetIpField = new JTextField(15);
        connectionPanel.add(targetIpField);
        
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToTarget());
        connectionPanel.add(connectButton);
        
        showQRButton = new JButton("Show QR Code");
        showQRButton.addActionListener(e -> showQRCode());
        connectionPanel.add(showQRButton);
        
        topPanel.add(connectionPanel);
        
        // Server Control Panel
        JPanel serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startServerButton = new JButton("Start Receiver");
        stopServerButton = new JButton("Stop Receiver");
        stopServerButton.setEnabled(false);
        
        startServerButton.addActionListener(e -> startServer());
        stopServerButton.addActionListener(e -> stopServer());
        
        serverPanel.add(startServerButton);
        serverPanel.add(stopServerButton);
        
        statusLabel = new JLabel("Receiver: Stopped");
        statusLabel.setForeground(Color.RED);
        serverPanel.add(statusLabel);
        
        topPanel.add(serverPanel);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Log Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Transfer Log"));
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        sendFileButton = new JButton("Send File");
        sendFileButton.addActionListener(e -> sendFile());
        bottomPanel.add(sendFileButton);
        
        openFolderButton = new JButton("Open Received Files");
        openFolderButton.addActionListener(e -> openReceivedFolder());
        bottomPanel.add(openFolderButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void connectToTarget() {
        String ip = targetIpField.getText().trim();
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an IP address", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Remove port if included
        if (ip.contains(":")) {
            ip = ip.split(":")[0];
        }
        
        log("Target set to: " + ip);
        JOptionPane.showMessageDialog(this, "Connected to " + ip, "Connected", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showQRCode() {
        String localIp = getLocalIpAddress();
        String connectionInfo = localIp + ":" + PORT;
        qrGenerator.showQRDialog(this, connectionInfo);
    }
    
    private void startServer() {
        receiver = new FileReceiver(logArea, statusLabel);
        receiver.start();
        startServerButton.setEnabled(false);
        stopServerButton.setEnabled(true);
        log("Receiver started on port " + PORT);
    }
    
    private void stopServer() {
        if (receiver != null) {
            receiver.stop();
            receiver = null;
        }
        startServerButton.setEnabled(true);
        stopServerButton.setEnabled(false);
        log("Receiver stopped");
    }
    
    private void sendFile() {
        String targetIp = targetIpField.getText().trim();
        if (targetIp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter target IP address", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (targetIp.contains(":")) {
            targetIp = targetIp.split(":")[0];
        }
        
        JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            sender = new FileSender(logArea, statusLabel);
            sender.sendFile(selectedFile, targetIp, PORT);
        }
    }
    
    private void openReceivedFolder() {
        try {
            File folder = new File(System.getProperty("user.home"), "SwiftShare");
            if (!folder.exists()) folder.mkdirs();
            Desktop.getDesktop().open(folder);
        } catch (Exception e) {
            log("Error opening folder: " + e.getMessage());
        }
    }
    
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
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
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwiftSharePC::new);
    }
}

package com.swiftshare.desktop;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class QRGenerator {
    
    public void showQRDialog(JFrame parent, String content) {
        JDialog dialog = new JDialog(parent, "Scan to Connect", true);
        dialog.setSize(350, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Scan this QR Code from SwiftShare Android");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Generate QR Code
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 250, 250);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            
            ImageIcon qrIcon = new ImageIcon(qrImage);
            JLabel qrLabel = new JLabel(qrIcon);
            qrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(qrLabel);
        } catch (WriterException e) {
            JLabel errorLabel = new JLabel("Error generating QR code");
            errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(errorLabel);
        }
        
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        JLabel ipLabel = new JLabel("IP: " + content);
        ipLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        ipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(ipLabel);
        
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JButton closeButton = new JButton("Close");
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> dialog.dispose());
        panel.add(closeButton);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
}

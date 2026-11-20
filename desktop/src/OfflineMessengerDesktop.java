package com.meshconnect.desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

/**
 * =========================================================================
 * Course: Advanced Enterprise Java Laboratory (CSE-4252)
 * Project: Offline Messenger - Enterprise Desktop Station
 * Technologies: Java Swing (JFrame), Pure JDBC (java.sql.*), Sockets (TCP 8888)
 * Database: SQLite (Embedded via JDBC Driver: org.sqlite.JDBC)
 * Student: Yeasaleh | ID: 0272310005101123 | 62nd Batch
 * Supervisor: Naima Sultana Ayesha, Lecturer, Dept of CSE, City University
 * =========================================================================
 */
public class OfflineMessengerDesktop extends JFrame {

    public static final int P2P_PORT = 8888;
    public static final byte TYPE_TEXT = 0x01;
    public static final byte TYPE_HANDSHAKE = 0x03;
    public static final byte TYPE_ACK = 0x06;

    // Database Connection URL
    private static final String DB_URL = "jdbc:sqlite:desktop_mesh_chat.db";

    // Colors matching WhatsApp Theme
    private static final Color COLOR_PRIMARY = new Color(7, 94, 84);       // #075E54
    private static final Color COLOR_ACCENT = new Color(18, 140, 126);     // #128C7E
    private static final Color COLOR_GREEN = new Color(37, 211, 102);      // #25D366
    private static final Color COLOR_BG = new Color(240, 242, 245);
    private static final Color COLOR_CHAT_BG = new Color(236, 229, 221);   // WhatsApp wallpaper tan
    private static final Color COLOR_SENT_MSG = new Color(220, 248, 198);  // #DCF8C6
    private static final Color COLOR_RECV_MSG = Color.WHITE;

    // UI Components
    private JLabel lblStatus;
    private JLabel lblLocalIp;
    private JTextField txtTargetIp;
    private JTextPane chatPane;
    private JTextField txtMessageInput;
    private JButton btnSend;
    private JTable tableDbLogs;
    private DefaultTableModel modelDbLogs;
    private JTextArea txtSocketLogs;
    private JLabel lblDbStatus;

    // Socket server thread
    private ServerSocket serverSocket;
    private volatile boolean isRunning = true;

    public OfflineMessengerDesktop() {
        super("Offline Messenger — Enterprise Desktop Station (CSE-4252 : JFrame & JDBC)");
        initDatabase();
        initUI();
        startP2PServer();
        loadChatHistoryFromJdbc();
        refreshDbTableFromJdbc();
    }

    // =========================================================================
    // 1. PURE JDBC (Java Database Connectivity) IMPLEMENTATION
    // =========================================================================

    /**
     * Establishes a raw JDBC connection using java.sql.DriverManager
     */
    public static Connection getJdbcConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver class not found: " + e.getMessage());
        }
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initializes relational tables via JDBC Statement
     */
    private void initDatabase() {
        String sqlCreateMessages = "CREATE TABLE IF NOT EXISTS desktop_messages ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "sender TEXT NOT NULL, "
                + "recipient_ip TEXT NOT NULL, "
                + "message_text TEXT NOT NULL, "
                + "timestamp TEXT NOT NULL, "
                + "status TEXT NOT NULL"
                + ");";

        String sqlCreatePeers = "CREATE TABLE IF NOT EXISTS desktop_peers ("
                + "ip TEXT PRIMARY KEY, "
                + "device_name TEXT NOT NULL, "
                + "last_seen TEXT NOT NULL"
                + ");";

        try (Connection conn = getJdbcConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlCreateMessages);
            stmt.execute(sqlCreatePeers);
            System.out.println("[JDBC] Database tables verified successfully via java.sql.Statement.");

        } catch (SQLException e) {
            System.err.println("[JDBC ERROR] Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Saves a message record to SQLite using java.sql.PreparedStatement
     */
    private void saveMessageViaJdbc(String sender, String recipientIp, String messageText, String timestamp, String status) {
        String insertSql = "INSERT INTO desktop_messages (sender, recipient_ip, message_text, timestamp, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getJdbcConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            pstmt.setString(1, sender);
            pstmt.setString(2, recipientIp);
            pstmt.setString(3, messageText);
            pstmt.setString(4, timestamp);
            pstmt.setString(5, status);

            int rowsAffected = pstmt.executeUpdate();
            logSocketEvent("[JDBC PreparedStatement] Inserted " + rowsAffected + " message record(s) into desktop_messages.");

        } catch (SQLException e) {
            System.err.println("[JDBC ERROR] Failed to save message: " + e.getMessage());
        }
    }

    /**
     * Records/updates a discovered peer node using java.sql.PreparedStatement
     */
    private void registerPeerViaJdbc(String ip, String deviceName, String lastSeen) {
        String upsertSql = "INSERT INTO desktop_peers (ip, device_name, last_seen) VALUES (?, ?, ?) "
                + "ON CONFLICT(ip) DO UPDATE SET device_name=excluded.device_name, last_seen=excluded.last_seen";

        try (Connection conn = getJdbcConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {

            pstmt.setString(1, ip);
            pstmt.setString(2, deviceName);
            pstmt.setString(3, lastSeen);
            pstmt.executeUpdate();
            logSocketEvent("[JDBC PreparedStatement] Peer node [" + deviceName + " (" + ip + ")] recorded in DB.");

        } catch (SQLException e) {
            System.err.println("[JDBC ERROR] Failed to register peer: " + e.getMessage());
        }
    }

    /**
     * Queries messages from SQLite via JDBC Statement and ResultSet for the JTable
     */
    private void refreshDbTableFromJdbc() {
        String querySql = "SELECT id, sender, recipient_ip, message_text, timestamp, status FROM desktop_messages ORDER BY id DESC";

        SwingUtilities.invokeLater(() -> {
            modelDbLogs.setRowCount(0);
            long startTime = System.currentTimeMillis();

            try (Connection conn = getJdbcConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(querySql)) {

                int count = 0;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String sender = rs.getString("sender");
                    String recipient = rs.getString("recipient_ip");
                    String msg = rs.getString("message_text");
                    String time = rs.getString("timestamp");
                    String status = rs.getString("status");

                    modelDbLogs.addRow(new Object[]{id, sender, recipient, msg, time, status});
                    count++;
                }

                long elapsed = System.currentTimeMillis() - startTime;
                lblDbStatus.setText("JDBC Status: " + count + " records queried via ResultSet in " + elapsed + " ms.");

            } catch (SQLException e) {
                lblDbStatus.setText("JDBC Query Error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Loads past chat conversation from SQLite on startup
     */
    private void loadChatHistoryFromJdbc() {
        String querySql = "SELECT sender, message_text, timestamp, status FROM desktop_messages ORDER BY id ASC LIMIT 50";

        try (Connection conn = getJdbcConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySql)) {

            while (rs.next()) {
                String sender = rs.getString("sender");
                String msg = rs.getString("message_text");
                String time = rs.getString("timestamp");
                boolean isMe = "Desktop Station (You)".equalsIgnoreCase(sender);
                appendChatMessage(sender, msg, time, isMe);
            }

        } catch (SQLException e) {
            System.err.println("[JDBC] Error loading chat history: " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. SWING GUI (JFrame) INITIALIZATION
    // =========================================================================

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 720);
        setMinimumSize(new Dimension(850, 600));
        setLocationRelativeTo(null);

        // Header Panel (Dark Teal)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_PRIMARY);
        headerPanel.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel lblTitle = new JLabel("Offline Messenger — Enterprise Desktop Station");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);

        String localIp = getLocalIpAddress();
        lblLocalIp = new JLabel("Station IP: " + localIp + " | Port: " + P2P_PORT + " | Engine: Java Sockets + JDBC");
        lblLocalIp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblLocalIp.setForeground(new Color(220, 248, 198));

        JPanel headerTextPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerTextPanel.setOpaque(false);
        headerTextPanel.add(lblTitle);
        headerTextPanel.add(lblLocalIp);

        lblStatus = new JLabel("● Listening for Android Peers");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatus.setForeground(COLOR_GREEN);

        headerPanel.add(headerTextPanel, BorderLayout.WEST);
        headerPanel.add(lblStatus, BorderLayout.EAST);

        // Main Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Live Chat & Sockets
        tabbedPane.addTab("💬 Live P2P Chat", createChatTabPanel());

        // Tab 2: Pure JDBC Database Explorer
        tabbedPane.addTab("🗄️ JDBC Database Explorer", createJdbcTabPanel());

        // Tab 3: Raw Socket Monitor
        tabbedPane.addTab("⚡ Raw Socket Console", createSocketLogTabPanel());

        // Assembly
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createChatTabPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Target IP Bar
        JPanel targetBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        targetBar.setBackground(Color.WHITE);
        targetBar.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));

        JLabel lblTarget = new JLabel("Target Android Phone IP:");
        lblTarget.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTarget.setForeground(new Color(51, 65, 85));

        txtTargetIp = new JTextField("192.168.49.1", 14);
        txtTargetIp.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnPing = new JButton("Test Socket Ping");
        btnPing.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPing.setBackground(COLOR_ACCENT);
        btnPing.setForeground(Color.WHITE);
        btnPing.setFocusPainted(false);
        btnPing.addActionListener(e -> pingTargetDevice());

        targetBar.add(lblTarget);
        targetBar.add(txtTargetIp);
        targetBar.add(btnPing);

        // Chat display area
        chatPane = new JTextPane();
        chatPane.setContentType("text/html");
        chatPane.setEditable(false);
        chatPane.setBackground(COLOR_CHAT_BG);
        chatPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollChat = new JScrollPane(chatPane);
        scrollChat.setBorder(new LineBorder(new Color(203, 213, 225), 1));

        // Input Panel at bottom
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(COLOR_BG);

        txtMessageInput = new JTextField();
        txtMessageInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMessageInput.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        txtMessageInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessageToPeer();
                }
            }
        });

        btnSend = new JButton("Send (TCP 8888)");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSend.setBackground(COLOR_PRIMARY);
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusPainted(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.addActionListener(e -> sendMessageToPeer());

        inputPanel.add(txtMessageInput, BorderLayout.CENTER);
        inputPanel.add(btnSend, BorderLayout.EAST);

        panel.add(targetBar, BorderLayout.NORTH);
        panel.add(scrollChat, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createJdbcTabPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Top Control bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new EmptyBorder(10, 14, 10, 14));

        lblDbStatus = new JLabel("JDBC Status: Ready");
        lblDbStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDbStatus.setForeground(COLOR_PRIMARY);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnRefresh = new JButton("🔄 Query DB (JDBC PreparedStatement)");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setBackground(COLOR_ACCENT);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> refreshDbTableFromJdbc());

        JButton btnClear = new JButton("🗑️ Clear Table via JDBC Statement");
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear.setBackground(new Color(220, 38, 38));
        btnClear.setForeground(Color.WHITE);
        btnClear.setFocusPainted(false);
        btnClear.addActionListener(e -> clearDatabaseViaJdbc());

        btnBar.add(btnRefresh);
        btnBar.add(btnClear);

        topBar.add(lblDbStatus, BorderLayout.WEST);
        topBar.add(btnBar, BorderLayout.EAST);

        // JDBC Data Table
        String[] columns = {"Record ID", "Sender", "Recipient IP", "Message Text", "Timestamp", "Delivery Status"};
        modelDbLogs = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableDbLogs = new JTable(modelDbLogs);
        tableDbLogs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableDbLogs.setRowHeight(26);
        tableDbLogs.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tableDbLogs.getTableHeader().setBackground(COLOR_PRIMARY);
        tableDbLogs.getTableHeader().setForeground(Color.WHITE);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableDbLogs.getColumnModel().getColumn(0).setPreferredWidth(70);
        tableDbLogs.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tableDbLogs.getColumnModel().getColumn(4).setPreferredWidth(140);
        tableDbLogs.getColumnModel().getColumn(5).setPreferredWidth(100);
        tableDbLogs.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        JScrollPane scrollTable = new JScrollPane(tableDbLogs);
        scrollTable.setBorder(new LineBorder(new Color(203, 213, 225), 1));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(scrollTable, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSocketLogTabPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        txtSocketLogs = new JTextArea();
        txtSocketLogs.setEditable(false);
        txtSocketLogs.setBackground(new Color(15, 23, 42)); // Dark Slate
        txtSocketLogs.setForeground(new Color(226, 232, 240));
        txtSocketLogs.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtSocketLogs.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollLogs = new JScrollPane(txtSocketLogs);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.setOpaque(false);

        JButton btnClearLog = new JButton("Clear Console");
        btnClearLog.addActionListener(e -> txtSocketLogs.setText(""));
        bottomBar.add(btnClearLog);

        panel.add(scrollLogs, BorderLayout.CENTER);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // 3. BACKGROUND TCP SERVER SOCKET (Listening for Android Phones on 8888)
    // =========================================================================

    private void startP2PServer() {
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(P2P_PORT);
                logSocketEvent("[Server] ServerSocket bound to port " + P2P_PORT + ". Listening for Android nodes...");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    logSocketEvent("[Connection] Inbound socket from Android phone: " + clientIp);

                    new Thread(() -> handleIncomingSocket(clientSocket, clientIp)).start();
                }

            } catch (IOException e) {
                if (isRunning) {
                    logSocketEvent("[Server Error] " + e.getMessage());
                }
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void handleIncomingSocket(Socket socket, String clientIp) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            byte packetType = dis.readByte();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            if (packetType == TYPE_TEXT) {
                // Read text packet [0x01][4-byte Length][UTF-8 Text]
                int len = dis.readInt();
                byte[] bytes = new byte[len];
                dis.readFully(bytes);
                String messageText = new String(bytes, StandardCharsets.UTF_8);

                // Send 0x06 ACK
                dos.writeByte(TYPE_ACK);
                dos.flush();

                logSocketEvent("[Packet 0x01 TEXT] Received from " + clientIp + ": \"" + messageText + "\" (Sent 0x06 ACK)");

                // Save to DB via Pure JDBC
                saveMessageViaJdbc("Android Peer (" + clientIp + ")", "Desktop Station", messageText, timestamp, "DELIVERED");

                // Update UI
                appendChatMessage("Android Peer (" + clientIp + ")", messageText, timestamp, false);
                refreshDbTableFromJdbc();

            } else if (packetType == TYPE_HANDSHAKE) {
                short nameLen = dis.readShort();
                byte[] nameBytes = new byte[nameLen];
                dis.readFully(nameBytes);
                String peerName = new String(nameBytes, StandardCharsets.UTF_8);

                dos.writeByte(TYPE_ACK);
                dos.flush();

                logSocketEvent("[Packet 0x03 HANDSHAKE] Peer Identified: " + peerName + " at " + clientIp);
                registerPeerViaJdbc(clientIp, peerName, timestamp);
            }

        } catch (IOException e) {
            logSocketEvent("[Socket Stream Error] " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // =========================================================================
    // 4. TRANSMISSION: SENDS TEXT PACKET TO ANDROID PHONE VIA SOCKET 8888
    // =========================================================================

    private void sendMessageToPeer() {
        String targetIp = txtTargetIp.getText().trim();
        String message = txtMessageInput.getText().trim();

        if (targetIp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the target Android device IP address.", "Target IP Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (message.isEmpty()) {
            return;
        }

        txtMessageInput.setText("");
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // Append to chat window immediately
        appendChatMessage("Desktop Station (You)", message, timestamp, true);

        // Send via background thread
        new Thread(() -> {
            boolean success = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(targetIp, P2P_PORT), 4000);
                socket.setSoTimeout(4000);

                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // Packet framing: [0x01][Length][UTF-8 Payload]
                byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
                dos.writeByte(TYPE_TEXT);
                dos.writeInt(bytes.length);
                dos.write(bytes);
                dos.flush();

                // Wait for ACK
                byte ack = dis.readByte();
                success = (ack == TYPE_ACK);

                logSocketEvent("[Send OK] Packet delivered to " + targetIp + " with ACK byte confirmation.");

            } catch (Exception e) {
                logSocketEvent("[Send Failed to " + targetIp + "] " + e.getMessage());
            }

            String status = success ? "DELIVERED" : "SENT_NO_ACK";
            // Commit to SQLite via JDBC PreparedStatement
            saveMessageViaJdbc("Desktop Station (You)", targetIp, message, timestamp, status);
            refreshDbTableFromJdbc();

        }).start();
    }

    private void pingTargetDevice() {
        String targetIp = txtTargetIp.getText().trim();
        if (targetIp.isEmpty()) return;

        new Thread(() -> {
            try (Socket socket = new Socket()) {
                long start = System.currentTimeMillis();
                socket.connect(new InetSocketAddress(targetIp, P2P_PORT), 3000);
                long latency = System.currentTimeMillis() - start;

                logSocketEvent("[Ping Success] Socket link active with " + targetIp + " on port " + P2P_PORT + " (" + latency + " ms)");
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Successfully connected to Android socket at " + targetIp + ":" + P2P_PORT + " in " + latency + " ms!",
                        "Socket Ping Successful", JOptionPane.INFORMATION_MESSAGE));

            } catch (Exception e) {
                logSocketEvent("[Ping Failed] Cannot reach " + targetIp + ":8888 -> " + e.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Could not reach Android socket at " + targetIp + ":" + P2P_PORT + "\nReason: " + e.getMessage(),
                        "Socket Ping Failed", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void clearDatabaseViaJdbc() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete all message records via JDBC Statement?",
                "Confirm JDBC Table Purge", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = getJdbcConnection();
                 Statement stmt = conn.createStatement()) {

                int deleted = stmt.executeUpdate("DELETE FROM desktop_messages;");
                stmt.execute("VACUUM;"); // Reclaims disk space

                logSocketEvent("[JDBC Statement] Executed table purge: " + deleted + " records deleted.");
                refreshDbTableFromJdbc();
                chatPane.setText("");

                JOptionPane.showMessageDialog(this, "Purged " + deleted + " records successfully via JDBC Statement.", "DB Cleared", JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "JDBC Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // 5. HELPER METHODS
    // =========================================================================

    private final StringBuilder chatHtmlBuilder = new StringBuilder();

    private synchronized void appendChatMessage(String sender, String text, String time, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            String bubbleColor = isMe ? "#DCF8C6" : "#FFFFFF";
            String align = isMe ? "right" : "left";

            String htmlSnippet = "<div style='text-align:" + align + "; margin-bottom:10px;'>"
                    + "<div style='display:inline-block; background-color:" + bubbleColor + "; padding:8px 12px; border-radius:10px; max-width:70%; text-align:left; border:1px solid #E2E8F0;'>"
                    + "<div style='font-size:11px; font-weight:bold; color:#075E54; margin-bottom:2px;'>" + escapeHtml(sender) + "</div>"
                    + "<div style='font-size:13px; color:#111B21;'>" + escapeHtml(text) + "</div>"
                    + "<div style='font-size:9px; color:#64748B; text-align:right; margin-top:4px;'>" + escapeHtml(time) + "</div>"
                    + "</div>"
                    + "</div>";

            chatHtmlBuilder.append(htmlSnippet);
            chatPane.setText("<html><body style='font-family:sans-serif; background-color:#ECE5DD; padding:10px;'>"
                    + chatHtmlBuilder.toString()
                    + "</body></html>");
            chatPane.setCaretPosition(chatPane.getDocument().getLength());
        });
    }

    private void logSocketEvent(String message) {
        String stamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        String logEntry = "[" + stamp + "] " + message + "\n";
        System.out.print(logEntry);
        SwingUtilities.invokeLater(() -> {
            if (txtSocketLogs != null) {
                txtSocketLogs.append(logEntry);
                txtSocketLogs.setCaretPosition(txtSocketLogs.getDocument().getLength());
            }
        });
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface nif = interfaces.nextElement();
                if (nif.isLoopback() || !nif.isUp()) continue;
                Enumeration<InetAddress> addresses = nif.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
    }

    public static void main(String[] args) {
        // Set Nimbus or System Look and Feel for clean appearance
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            OfflineMessengerDesktop station = new OfflineMessengerDesktop();
            station.setVisible(true);
        });
    }
}

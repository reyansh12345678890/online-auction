import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public class Main extends JFrame {
    // Database settings are supplied by the runtime environment.
    private static final String JDBC_URL = requiredSetting("JDBC_URL");
    private static final String USERNAME = requiredSetting("DB_USERNAME");
    private static final String PASSWORD = requiredSetting("DB_PASSWORD");

    // Database connection
    private Connection connection;

    private static String requiredSetting(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    // UI Components
    private JTabbedPane tabbedPane;
    private JPanel sellerPanel, customerPanel, productPanel, auctionPanel;

    // Format for currency display
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

    // Constructor
    public Main() {
        // Set up the JFrame
        super("Auction System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // Set app icon and look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // IMPORTANT: First connect to database before initializing UI
        connectToDatabase();

        // Create tables if they don't exist
        createTablesIfNotExist();

        // Then initialize UI
        initUI();

        // Load initial data
        refreshAllTables();

        // Set visible
        setVisible(true);
    }

    // Initialize UI components
    private void initUI() {
        // Create tabbed pane with custom styling
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setBackground(new Color(245, 245, 245));

        // Create panels
        createSellerPanel();
        createCustomerPanel();
        createProductPanel();
        createAuctionPanel();

        // Add panels to tabbed pane
        tabbedPane.addTab("Auctions", new ImageIcon(), auctionPanel, "Manage Auctions");
        tabbedPane.addTab("Products", new ImageIcon(), productPanel, "Manage Products");
        tabbedPane.addTab("Customers", new ImageIcon(), customerPanel, "Manage Customers");
        tabbedPane.addTab("Sellers", new ImageIcon(), sellerPanel, "Manage Sellers");

        // Add tabbed pane to frame
        add(tabbedPane);

        // Create status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EtchedBorder());
        JLabel statusLabel = new JLabel(" Auction System - Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);
    }

    // Create seller panel
    private void createSellerPanel() {
        sellerPanel = new JPanel(new BorderLayout(10, 10));
        sellerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Seller Information",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Seller ID field (hidden for new entries)
        JTextField txtSellerId = new JTextField(10);
        txtSellerId.setVisible(false);

        // Name field
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtSellerName = new JTextField(20);
        formPanel.add(txtSellerName, gbc);

        // Email field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtSellerEmail = new JTextField(20);
        formPanel.add(txtSellerEmail, gbc);

        // Phone field
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtSellerPhone = new JTextField(20);
        formPanel.add(txtSellerPhone, gbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnAddSeller = new JButton("Add Seller");
        btnAddSeller.setIcon(new ImageIcon());
        JButton btnUpdateSeller = new JButton("Update");
        JButton btnDeleteSeller = new JButton("Delete");
        JButton btnClearSeller = new JButton("Clear Form");

        buttonPanel.add(btnAddSeller);
        buttonPanel.add(btnUpdateSeller);
        buttonPanel.add(btnDeleteSeller);
        buttonPanel.add(btnClearSeller);

        // Table panel
        sellerTableModel = new DefaultTableModel(
                new Object[][] {},
                new String[] {"ID", "Name", "Email", "Phone"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable sellerTable = new JTable(sellerTableModel);
        sellerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sellerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        sellerTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sellerTable.setRowHeight(25);

        JScrollPane tableScrollPane = new JScrollPane(sellerTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Sellers List",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        JTextField txtSellerSearch = new JTextField(20);
        searchPanel.add(txtSellerSearch);
        JButton btnSellerSearch = new JButton("Search");
        searchPanel.add(btnSellerSearch);
        JButton btnSellerReset = new JButton("Reset");
        searchPanel.add(btnSellerReset);

        // Assemble panels
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(formPanel, BorderLayout.CENTER);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        sellerPanel.add(northPanel, BorderLayout.NORTH);
        sellerPanel.add(centerPanel, BorderLayout.CENTER);

        // Add event listeners

        // Table selection listener
        sellerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = sellerTable.getSelectedRow();
                if (selectedRow != -1) {
                    txtSellerId.setText(sellerTable.getValueAt(selectedRow, 0).toString());
                    txtSellerName.setText(sellerTable.getValueAt(selectedRow, 1).toString());
                    txtSellerEmail.setText(sellerTable.getValueAt(selectedRow, 2).toString());
                    txtSellerPhone.setText(sellerTable.getValueAt(selectedRow, 3).toString());
                }
            }
        });

        // Add seller button
        btnAddSeller.addActionListener(e -> {
            if (txtSellerName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seller name cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                String query = "INSERT INTO sellers (name, email, phone) VALUES (?, ?, ?)";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, txtSellerName.getText().trim());
                pstmt.setString(2, txtSellerEmail.getText().trim());
                pstmt.setString(3, txtSellerPhone.getText().trim());

                int result = pstmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Seller added successfully");
                    clearSellerForm();
                    loadSellers(sellerTableModel);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding seller: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // Update seller button
        btnUpdateSeller.addActionListener(e -> {
            if (txtSellerId.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a seller to update", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (txtSellerName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seller name cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                String query = "UPDATE sellers SET name = ?, email = ?, phone = ? WHERE id = ?";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, txtSellerName.getText().trim());
                pstmt.setString(2, txtSellerEmail.getText().trim());
                pstmt.setString(3, txtSellerPhone.getText().trim());
                pstmt.setInt(4, Integer.parseInt(txtSellerId.getText()));

                int result = pstmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Seller updated successfully");
                    clearSellerForm();
                    loadSellers(sellerTableModel);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error updating seller: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // Delete seller button
        btnDeleteSeller.addActionListener(e -> {
            if (txtSellerId.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a seller to delete", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete this seller?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String query = "DELETE FROM sellers WHERE id = ?";
                    PreparedStatement pstmt = connection.prepareStatement(query);
                    pstmt.setInt(1, Integer.parseInt(txtSellerId.getText()));

                    int result = pstmt.executeUpdate();
                    if (result > 0) {
                        JOptionPane.showMessageDialog(this, "Seller deleted successfully");
                        clearSellerForm();
                        loadSellers(sellerTableModel);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error deleting seller: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        // Clear form button
        btnClearSeller.addActionListener(e -> {
            clearSellerForm();
            sellerTable.clearSelection();
        });

        // Search button
        btnSellerSearch.addActionListener(e -> {
            String searchTerm = txtSellerSearch.getText().trim();
            if (!searchTerm.isEmpty()) {
                try {
                    String query = "SELECT * FROM sellers WHERE name LIKE ? OR email LIKE ? OR phone LIKE ?";
                    PreparedStatement pstmt = connection.prepareStatement(query);
                    String term = "%" + searchTerm + "%";
                    pstmt.setString(1, term);
                    pstmt.setString(2, term);
                    pstmt.setString(3, term);

                    ResultSet rs = pstmt.executeQuery();

                    sellerTableModel.setRowCount(0);

                    while (rs.next()) {
                        sellerTableModel.addRow(new Object[] {
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("email"),
                                rs.getString("phone")
                        });
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Reset search button
        btnSellerReset.addActionListener(e -> {
            txtSellerSearch.setText("");
            loadSellers(sellerTableModel);
        });

        // Helper method to clear form
        Runnable clearSellerForm = () -> {
            txtSellerId.setText("");
            txtSellerName.setText("");
            txtSellerEmail.setText("");
            txtSellerPhone.setText("");
        };
    }

    // Create customer panel
    private void createCustomerPanel() {
        customerPanel = new JPanel(new BorderLayout(10, 10));
        customerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Customer Information",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Customer ID field (hidden for new entries)
        JTextField txtCustomerId = new JTextField(10);
        txtCustomerId.setVisible(false);

        // Name field
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtCustomerName = new JTextField(20);
        formPanel.add(txtCustomerName, gbc);

        // Email field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtCustomerEmail = new JTextField(20);
        formPanel.add(txtCustomerEmail, gbc);

        // Phone field
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtCustomerPhone = new JTextField(20);
        formPanel.add(txtCustomerPhone, gbc);

        // Address field
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtCustomerAddress = new JTextField(20);
        formPanel.add(txtCustomerAddress, gbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnAddCustomer = new JButton("Add Customer");
        JButton btnUpdateCustomer = new JButton("Update");
        JButton btnDeleteCustomer = new JButton("Delete");
        JButton btnClearCustomer = new JButton("Clear Form");

        buttonPanel.add(btnAddCustomer);
        buttonPanel.add(btnUpdateCustomer);
        buttonPanel.add(btnDeleteCustomer);
        buttonPanel.add(btnClearCustomer);

        // Table panel
        customerTableModel = new DefaultTableModel(
                new Object[][] {},
                new String[] {"ID", "Name", "Email", "Phone", "Address"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable customerTable = new JTable(customerTableModel);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        customerTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        customerTable.setRowHeight(25);

        JScrollPane tableScrollPane = new JScrollPane(customerTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Customers List",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        JTextField txtCustomerSearch = new JTextField(20);
        searchPanel.add(txtCustomerSearch);
        JButton btnCustomerSearch = new JButton("Search");
        searchPanel.add(btnCustomerSearch);
        JButton btnCustomerReset = new JButton("Reset");
        searchPanel.add(btnCustomerReset);

        // Assemble panels
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(formPanel, BorderLayout.CENTER);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        customerPanel.add(northPanel, BorderLayout.NORTH);
        customerPanel.add(centerPanel, BorderLayout.CENTER);

        // Add event listeners

        // Table selection listener
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = customerTable.getSelectedRow();
                if (selectedRow != -1) {
                    txtCustomerId.setText(customerTable.getValueAt(selectedRow, 0).toString());
                    txtCustomerName.setText(customerTable.getValueAt(selectedRow, 1).toString());
                    txtCustomerEmail.setText(customerTable.getValueAt(selectedRow, 2).toString());
                    txtCustomerPhone.setText(customerTable.getValueAt(selectedRow, 3).toString());
                    txtCustomerAddress.setText(customerTable.getValueAt(selectedRow, 4).toString());
                }
            }
        });

        // Add customer button
        btnAddCustomer.addActionListener(e -> {
            if (txtCustomerName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Customer name cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                String query = "INSERT INTO customers (name, email, phone, address) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, txtCustomerName.getText().trim());
                pstmt.setString(2, txtCustomerEmail.getText().trim());
                pstmt.setString(3, txtCustomerPhone.getText().trim());
                pstmt.setString(4, txtCustomerAddress.getText().trim());

                int result = pstmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Customer added successfully");
                    clearCustomerForm();
                    loadCustomers(customerTableModel);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding customer: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // Update customer button
        btnUpdateCustomer.addActionListener(e -> {
            if (txtCustomerId.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a customer to update", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (txtCustomerName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Customer name cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                String query = "UPDATE customers SET name = ?, email = ?, phone = ?, address = ? WHERE id = ?";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, txtCustomerName.getText().trim());
                pstmt.setString(2, txtCustomerEmail.getText().trim());
                pstmt.setString(3, txtCustomerPhone.getText().trim());
                pstmt.setString(4, txtCustomerAddress.getText().trim());
                pstmt.setInt(5, Integer.parseInt(txtCustomerId.getText()));

                int result = pstmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Customer updated successfully");
                    clearCustomerForm();
                    loadCustomers(customerTableModel);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error updating customer: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // Delete customer button
        btnDeleteCustomer.addActionListener(e -> {
            if (txtCustomerId.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a customer to delete", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete this customer?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String query = "DELETE FROM customers WHERE id = ?";
                    PreparedStatement pstmt = connection.prepareStatement(query);
                    pstmt.setInt(1, Integer.parseInt(txtCustomerId.getText()));

                    int result = pstmt.executeUpdate();
                    if (result > 0) {
                        JOptionPane.showMessageDialog(this, "Customer deleted successfully");
                        clearCustomerForm();
                        loadCustomers(customerTableModel);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error deleting customer: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        // Clear form button
        btnClearCustomer.addActionListener(e -> {
            clearCustomerForm();
            customerTable.clearSelection();
        });

        // Search button
        btnCustomerSearch.addActionListener(e -> {
            String searchTerm = txtCustomerSearch.getText().trim();
            if (!searchTerm.isEmpty()) {
                try {
                    String query = "SELECT * FROM customers WHERE name LIKE ? OR email LIKE ? OR phone LIKE ? OR address LIKE ?";
                    PreparedStatement pstmt = connection.prepareStatement(query);
                    String term = "%" + searchTerm + "%";
                    pstmt.setString(1, term);
                    pstmt.setString(2, term);
                    pstmt.setString(3, term);
                    pstmt.setString(4, term);

                    ResultSet rs = pstmt.executeQuery();

                    customerTableModel.setRowCount(0);

                    while (rs.next()) {
                        customerTableModel.addRow(new Object[] {
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("email"),
                                rs.getString("phone"),
                                rs.getString("address")
                        });
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Reset search button
        btnCustomerReset.addActionListener(e -> {
            txtCustomerSearch.setText("");
            loadCustomers(customerTableModel);
        });

        // Helper method to clear form
        Runnable clearCustomerForm = () -> {
            txtCustomerId.setText("");
            txtCustomerName.setText("");
            txtCustomerEmail.setText("");
            txtCustomerPhone.setText("");
            txtCustomerAddress.setText("");
        };
    }

    // Create product panel
    private void createProductPanel() {
        productPanel = new JPanel(new BorderLayout(10, 10));
        productPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Product Information",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Product ID field (hidden for new entries)
        JTextField txtProductId = new JTextField(10);
        txtProductId.setVisible(false);

        // Name field
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtProductName = new JTextField(20);
        formPanel.add(txtProductName, gbc);

        // Description field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextArea txtProductDescription = new JTextArea(3, 20);
        txtProductDescription.setLineWrap(true);
        JScrollPane scrollDescription = new JScrollPane(txtProductDescription);
        formPanel.add(scrollDescription, gbc);

        // Starting Price field
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Starting Price:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtProductStartPrice = new JTextField(20);
        formPanel.add(txtProductStartPrice, gbc);

        // Seller field
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Seller:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<String> cbProductSeller = new JComboBox<>();
        formPanel.add(cbProductSeller, gbc);

        // Category field
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<String> cbProductCategory = new JComboBox<>(
                new String[] {"Art", "Electronics", "Fashion", "Home & Garden", "Jewelry", "Sports", "Other"}
        );
        formPanel.add(cbProductCategory, gbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnAddProduct = new JButton("Add Product");
        JButton btnUpdateProduct = new JButton("Update");
        JButton btnDeleteProduct = new JButton("Delete");
        JButton btnClearProduct = new JButton("Clear Form");

        buttonPanel.add(btnAddProduct);
        buttonPanel.add(btnUpdateProduct);
        buttonPanel.add(btnDeleteProduct);
        buttonPanel.add(btnClearProduct);

        // Table panel
        productTableModel = new DefaultTableModel(
                new Object[][] {},
                new String[] {"ID", "Name", "Description", "Starting Price", "Seller", "Category"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable productTable = new JTable(productTableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        productTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        productTable.setRowHeight(25);

        JScrollPane tableScrollPane = new JScrollPane(productTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Products List",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        JTextField txtProductSearch = new JTextField(20);
        searchPanel.add(txtProductSearch);
        JButton btnProductSearch = new JButton("Search");
        searchPanel.add(btnProductSearch);
        JButton btnProductReset = new JButton("Reset");
        searchPanel.add(btnProductReset);

        // Assemble panels
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(formPanel, BorderLayout.CENTER);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        productPanel.add(northPanel, BorderLayout.NORTH);
        productPanel.add(centerPanel, BorderLayout.CENTER);

        // Load sellers for combo box
        loadSellersCombo(cbProductSeller);

        // Add event listeners

        // Table selection listener
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = productTable.getSelectedRow();
                if (selectedRow != -1) {
                    txtProductId.setText(productTable.getValueAt(selectedRow, 0).toString());
                    txtProductName.setText(productTable.getValueAt(selectedRow, 1).toString());
                    txtProductDescription.setText(productTable.getValueAt(selectedRow, 2).toString());
                    txtProductStartPrice.setText(productTable.getValueAt(selectedRow, 3).toString().replace("$", ""));
                    cbProductSeller.setSelectedItem(productTable.getValueAt(selectedRow, 4));
                    cbProductCategory.setSelectedItem(productTable.getValueAt(selectedRow, 5));
                }
            }
        });

        // Add product button
        btnAddProduct.addActionListener(e -> {
            // Validate input
            if (txtProductName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Product name cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (cbProductSeller.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Please select a seller", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate price
            double startPrice;
            try {
                startPrice = Double.parseDouble(txtProductStartPrice.getText().trim());
                if (startPrice <= 0) {
                    JOptionPane.showMessageDialog(this, "Starting price must be greater than zero", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid starting price", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Get seller ID
                int sellerId = getSellerIdByName(cbProductSeller.getSelectedItem().toString());

                String query = "INSERT INTO products (name, description, start_price, seller_id, category) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, txtProductName.getText().trim());
                pstmt.setString(2, txtProductDescription.getText().trim());
                pstmt.setDouble(3, startPrice);
                pstmt.setInt(4, sellerId);
                pstmt.setString(5, cbProductCategory.getSelectedItem().toString());

                int result = pstmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Product added successfully");
                    clearProductForm();
                    loadProducts(productTableModel);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding product: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // Update product button
        btnUpdateProduct.addActionListener(e -> {
            if (txtProductId.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a product to update", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (txtProductName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Product name cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate price
            double startPrice;
            try {
                startPrice = Double.parseDouble(txtProductStartPrice.getText().trim());
                if (startPrice <= 0) {
                    JOptionPane.showMessageDialog(this, "Starting price must be greater than zero", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid starting price", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Get seller ID
                int sellerId = getSellerIdByName(cbProductSeller.getSelectedItem().toString());

                String query = "UPDATE products SET name = ?, description = ?, start_price = ?, seller_id = ?, category = ? WHERE id = ?";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, txtProductName.getText().trim());
                pstmt.setString(2, txtProductDescription.getText().trim());
                pstmt.setDouble(3, startPrice);
                pstmt.setInt(4, sellerId);
                pstmt.setString(5, cbProductCategory.getSelectedItem().toString());
                pstmt.setInt(6, Integer.parseInt(txtProductId.getText()));

                int result = pstmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Product updated successfully");
                    clearProductForm();
                    loadProducts(productTableModel);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error updating product: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // Delete product button
        btnDeleteProduct.addActionListener(e -> {
            if (txtProductId.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a product to delete", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete this product?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String query = "DELETE FROM products WHERE id = ?";
                    PreparedStatement pstmt = connection.prepareStatement(query);
                    pstmt.setInt(1, Integer.parseInt(txtProductId.getText()));

                    int result = pstmt.executeUpdate();
                    if (result > 0) {
                        JOptionPane.showMessageDialog(this, "Product deleted successfully");
                        clearProductForm();
                        loadProducts(productTableModel);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error deleting product: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        // Clear form button
        btnClearProduct.addActionListener(e -> {
            clearProductForm();
            productTable.clearSelection();
        });

        // Search button
        btnProductSearch.addActionListener(e -> {
            String searchTerm = txtProductSearch.getText().trim();
            if (!searchTerm.isEmpty()) {
                try {
                    String query = "SELECT p.id, p.name, p.description, p.start_price, s.name as seller_name, p.category " +
                            "FROM products p JOIN sellers s ON p.seller_id = s.id " +
                            "WHERE p.name LIKE ? OR p.description LIKE ? OR p.category LIKE ? OR s.name LIKE ?";
                    PreparedStatement pstmt = connection.prepareStatement(query);
                    String term = "%" + searchTerm + "%";
                    pstmt.setString(1, term);
                    pstmt.setString(2, term);
                    pstmt.setString(3, term);
                    pstmt.setString(4, term);

                    ResultSet rs = pstmt.executeQuery();

                    productTableModel.setRowCount(0);

                    while (rs.next()) {
                        productTableModel.addRow(new Object[] {
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("description"),
                                currencyFormat.format(rs.getDouble("start_price")),
                                rs.getString("seller_name"),
                                rs.getString("category")
                        });
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Reset search button
        btnProductReset.addActionListener(e -> {
            txtProductSearch.setText("");
            loadProducts(productTableModel);
        });

        // Helper method to clear form
        Runnable clearProductForm = () -> {
            txtProductId.setText("");
            txtProductName.setText("");
            txtProductDescription.setText("");
            txtProductStartPrice.setText("");
            cbProductSeller.setSelectedIndex(0);
            cbProductCategory.setSelectedIndex(0);
        };
    }

    // Create auction panel
    private void createAuctionPanel() {
        auctionPanel = new JPanel(new BorderLayout(10, 10));
        auctionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Auction Information",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Auction ID field (hidden for new entries)
        JTextField txtAuctionId = new JTextField(10);
        txtAuctionId.setVisible(false);

        // Product field
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Product:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<String> cbAuctionProduct = new JComboBox<>();
        formPanel.add(cbAuctionProduct, gbc);

        // Current bid field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Current Bid:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtAuctionCurrentBid = new JTextField(20);
        txtAuctionCurrentBid.setEditable(false);
        formPanel.add(txtAuctionCurrentBid, gbc);

        // Bid amount field
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("New Bid Amount:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField txtAuctionBidAmount = new JTextField(20);
        formPanel.add(txtAuctionBidAmount, gbc);

        // Bidder field
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Bidder:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<String> cbAuctionBidder = new JComboBox<>();
        formPanel.add(cbAuctionBidder, gbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnStartAuction = new JButton("Start Auction");
        JButton btnPlaceBid = new JButton("Place Bid");
        JButton btnEndAuction = new JButton("End Auction");
        JButton btnClearAuction = new JButton("Clear Form");

        buttonPanel.add(btnStartAuction);
        buttonPanel.add(btnPlaceBid);
        buttonPanel.add(btnEndAuction);
        buttonPanel.add(btnClearAuction);

        // Table panel
        auctionTableModel = new DefaultTableModel(
                new Object[][] {},
                new String[] {"ID", "Product", "Start Price", "Current Bid", "Highest Bidder", "Status"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable auctionTable = new JTable(auctionTableModel);
        auctionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        auctionTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        auctionTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        auctionTable.setRowHeight(25);

        JScrollPane tableScrollPane = new JScrollPane(auctionTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Active Auctions",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));

        // Bid history panel
        DefaultTableModel bidHistoryModel = new DefaultTableModel(
                new Object[][] {},
                new String[] {"Bid ID", "Amount", "Bidder", "Bid Time"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable bidHistoryTable = new JTable(bidHistoryModel);
        bidHistoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bidHistoryTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        bidHistoryTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bidHistoryTable.setRowHeight(25);

        JScrollPane bidHistoryScrollPane = new JScrollPane(bidHistoryTable);
        bidHistoryScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Bid History",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> cbAuctionStatusFilter = new JComboBox<>(new String[] {"All Auctions", "Active Only", "Ended Only"});
        filterPanel.add(new JLabel("Show:"));
        filterPanel.add(cbAuctionStatusFilter);
        JButton btnAuctionFilter = new JButton("Apply Filter");
        filterPanel.add(btnAuctionFilter);

        // Split pane for tables
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, bidHistoryScrollPane);
        splitPane.setResizeWeight(0.6);

        // Assemble panels
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(formPanel, BorderLayout.CENTER);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(filterPanel, BorderLayout.NORTH);
        centerPanel.add(splitPane, BorderLayout.CENTER);

        auctionPanel.add(northPanel, BorderLayout.NORTH);
        auctionPanel.add(centerPanel, BorderLayout.CENTER);

        // Load comboboxes
        loadProductsCombo(cbAuctionProduct);
        loadCustomersCombo(cbAuctionBidder);

        // Add event listeners

        // Table selection listener
        auctionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = auctionTable.getSelectedRow();
                if (selectedRow != -1) {
                    txtAuctionId.setText(auctionTable.getValueAt(selectedRow, 0).toString());
                    cbAuctionProduct.setSelectedItem(auctionTable.getValueAt(selectedRow, 1));

                    // Set current bid
                    String currentBid = auctionTable.getValueAt(selectedRow, 3).toString();
                    txtAuctionCurrentBid.setText(currentBid);

                    // Load bid history
                    loadBidHistory(Integer.parseInt(txtAuctionId.getText()), bidHistoryModel);
                }
            }
        });

        // Start auction button
        btnStartAuction.addActionListener(e -> {
            if (cbAuctionProduct.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Please select a product", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Get product ID and start price
                int productId = getProductIdByName(cbAuctionProduct.getSelectedItem().toString());
                double startPrice = getProductStartPrice(productId);

                // Check if product is already in an active auction
                if (isProductInActiveAuction(productId)) {
                    JOptionPane.showMessageDialog(this, "This product is already in an active auction", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String query = "INSERT INTO auctions (product_id, current_bid, status) VALUES (?, ?, 'Active')";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setInt(1, productId);
                pstmt.setDouble(2, startPrice);

                int result = pstmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Auction started successfully");
                    clearAuctionForm();
                    loadAuctions(auctionTableModel);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error starting auction: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // Place bid button
        btnPlaceBid.addActionListener(e -> {
            if (txtAuctionId.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select an auction", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (cbAuctionBidder.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Please select a bidder", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate bid amount
            double bidAmount;
            try {
                bidAmount = Double.parseDouble(txtAuctionBidAmount.getText().trim());
                double currentBid = Double.parseDouble(txtAuctionCurrentBid.getText().trim().replace("$", "").replace(",", ""));

                if (bidAmount <= currentBid) {
                    JOptionPane.showMessageDialog(this, "Bid amount must be greater than the current bid", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid bid amount", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Get customer ID
                int customerId = getCustomerIdByName(cbAuctionBidder.getSelectedItem().toString());
                int auctionId = Integer.parseInt(txtAuctionId.getText());

                // Begin transaction
                connection.setAutoCommit(false);

                // Insert bid record
                String bidQuery = "INSERT INTO bids (auction_id, customer_id, amount, bid_time) VALUES (?, ?, ?, NOW())";
                PreparedStatement bidStmt = connection.prepareStatement(bidQuery);
                bidStmt.setInt(1, auctionId);
                bidStmt.setInt(2, customerId);
                bidStmt.setDouble(3, bidAmount);
                bidStmt.executeUpdate();

                // Update auction current bid and highest bidder
                String updateQuery = "UPDATE auctions SET current_bid = ?, highest_bidder_id = ? WHERE id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setDouble(1, bidAmount);
                updateStmt.setInt(2, customerId);
                updateStmt.setInt(3, auctionId);
                updateStmt.executeUpdate();

                // Commit transaction
                connection.commit();

                JOptionPane.showMessageDialog(this, "Bid placed successfully");
                txtAuctionBidAmount.setText("");
                loadAuctions(auctionTableModel);
                loadBidHistory(auctionId, bidHistoryModel);

                // Update current bid display
                txtAuctionCurrentBid.setText(currencyFormat.format(bidAmount));

                // Reset auto-commit
                connection.setAutoCommit(true);

            } catch (SQLException ex) {
                try {
                    // Rollback transaction on error
                    connection.rollback();
                    connection.setAutoCommit(true);
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }

                JOptionPane.showMessageDialog(this, "Error placing bid: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        // End auction button
        btnEndAuction.addActionListener(e -> {
            if (txtAuctionId.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select an auction to end", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to end this auction?",
                    "Confirm End Auction", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String query = "UPDATE auctions SET status = 'Ended', end_time = NOW() WHERE id = ?";
                    PreparedStatement pstmt = connection.prepareStatement(query);
                    pstmt.setInt(1, Integer.parseInt(txtAuctionId.getText()));

                    int result = pstmt.executeUpdate();
                    if (result > 0) {
                        JOptionPane.showMessageDialog(this, "Auction ended successfully");
                        clearAuctionForm();
                        loadAuctions(auctionTableModel);
                        bidHistoryModel.setRowCount(0);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error ending auction: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        // Clear form button
        btnClearAuction.addActionListener(e -> {
            clearAuctionForm();
            auctionTable.clearSelection();
            bidHistoryModel.setRowCount(0);
        });

        // Filter button
        btnAuctionFilter.addActionListener(e -> {
            String status = cbAuctionStatusFilter.getSelectedItem().toString();
            loadAuctions(auctionTableModel, status);
        });

        // Helper method to clear form
        Runnable clearAuctionForm = () -> {
            txtAuctionId.setText("");
            txtAuctionCurrentBid.setText("");
            txtAuctionBidAmount.setText("");
            if (cbAuctionProduct.getItemCount() > 0) {
                cbAuctionProduct.setSelectedIndex(0);
            }
            if (cbAuctionBidder.getItemCount() > 0) {
                cbAuctionBidder.setSelectedIndex(0);
            }
        };
    }

    // Connect to database
    private void connectToDatabase() {
        try {
            // Load JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Connect to database
            connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);

            System.out.println("Connected to database successfully");
        } catch (ClassNotFoundException | SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Database connection error: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Create database tables if they don't exist
    private void createTablesIfNotExist() {
        try {
            Statement stmt = connection.createStatement();

            // Create sellers table
            String createSellersTable = "CREATE TABLE IF NOT EXISTS sellers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100), " +
                    "phone VARCHAR(20)" +
                    ")";
            stmt.executeUpdate(createSellersTable);

            // Create customers table
            String createCustomersTable = "CREATE TABLE IF NOT EXISTS customers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100), " +
                    "phone VARCHAR(20), " +
                    "address VARCHAR(200)" +
                    ")";
            stmt.executeUpdate(createCustomersTable);

            // Create products table
            String createProductsTable = "CREATE TABLE IF NOT EXISTS products (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "description TEXT, " +
                    "start_price DOUBLE NOT NULL, " +
                    "seller_id INT NOT NULL, " +
                    "category VARCHAR(50), " +
                    "FOREIGN KEY (seller_id) REFERENCES sellers(id)" +
                    ")";
            stmt.executeUpdate(createProductsTable);

            // Create auctions table
            String createAuctionsTable = "CREATE TABLE IF NOT EXISTS auctions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "product_id INT NOT NULL, " +
                    "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "end_time TIMESTAMP NULL, " +
                    "current_bid DOUBLE NOT NULL, " +
                    "highest_bidder_id INT NULL, " +
                    "status VARCHAR(20) NOT NULL, " +
                    "FOREIGN KEY (product_id) REFERENCES products(id), " +
                    "FOREIGN KEY (highest_bidder_id) REFERENCES customers(id)" +
                    ")";
            stmt.executeUpdate(createAuctionsTable);

            // Create bids table
            String createBidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "auction_id INT NOT NULL, " +
                    "customer_id INT NOT NULL, " +
                    "amount DOUBLE NOT NULL, " +
                    "bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (auction_id) REFERENCES auctions(id), " +
                    "FOREIGN KEY (customer_id) REFERENCES customers(id)" +
                    ")";
            stmt.executeUpdate(createBidsTable);

            stmt.close();
            System.out.println("Database tables created successfully");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error creating database tables: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Declare table models as class members so we can access them easily
    private DefaultTableModel sellerTableModel;
    private DefaultTableModel customerTableModel;
    private DefaultTableModel productTableModel;
    private DefaultTableModel auctionTableModel;

    // Refresh all tables
    private void refreshAllTables() {
        // Load data using the stored table models
        loadSellers(sellerTableModel);
        loadCustomers(customerTableModel);
        loadProducts(productTableModel);
        loadAuctions(auctionTableModel);
    }

    // Load sellers
    private void loadSellers(DefaultTableModel model) {
        model.setRowCount(0);

        try {
            String query = "SELECT * FROM sellers ORDER BY name";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone")
                });
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load customers
    private void loadCustomers(DefaultTableModel model) {
        model.setRowCount(0);

        try {
            String query = "SELECT * FROM customers ORDER BY name";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address")
                });
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load products
    private void loadProducts(DefaultTableModel model) {
        model.setRowCount(0);

        try {
            String query = "SELECT p.*, s.name as seller_name FROM products p JOIN sellers s ON p.seller_id = s.id ORDER BY p.name";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        currencyFormat.format(rs.getDouble("start_price")),
                        rs.getString("seller_name"),
                        rs.getString("category")
                });
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load auctions
    private void loadAuctions(DefaultTableModel model) {
        loadAuctions(model, "All Auctions");
    }

    private void loadAuctions(DefaultTableModel model, String filter) {
        model.setRowCount(0);

        try {
            StringBuilder queryBuilder = new StringBuilder(
                    "SELECT a.*, p.name as product_name, p.start_price, c.name as bidder_name " +
                            "FROM auctions a " +
                            "JOIN products p ON a.product_id = p.id " +
                            "LEFT JOIN customers c ON a.highest_bidder_id = c.id "
            );

            if (!filter.equals("All Auctions")) {
                queryBuilder.append("WHERE a.status = '");
                queryBuilder.append(filter.equals("Active Only") ? "Active" : "Ended");
                queryBuilder.append("' ");
            }

            queryBuilder.append("ORDER BY a.start_time DESC");

            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(queryBuilder.toString());

            while (rs.next()) {
                String highestBidder = rs.getString("bidder_name");
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("product_name"),
                        currencyFormat.format(rs.getDouble("start_price")),
                        currencyFormat.format(rs.getDouble("current_bid")),
                        highestBidder != null ? highestBidder : "No bids yet",
                        rs.getString("status")
                });
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load bid history
    private void loadBidHistory(int auctionId, DefaultTableModel model) {
        model.setRowCount(0);

        try {
            String query = "SELECT b.*, c.name as bidder_name FROM bids b " +
                    "JOIN customers c ON b.customer_id = c.id " +
                    "WHERE b.auction_id = ? ORDER BY b.bid_time DESC";

            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, auctionId);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        currencyFormat.format(rs.getDouble("amount")),
                        rs.getString("bidder_name"),
                        rs.getTimestamp("bid_time")
                });
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load sellers into combo box
    private void loadSellersCombo(JComboBox<String> comboBox) {
        comboBox.removeAllItems();

        try {
            String query = "SELECT name FROM sellers ORDER BY name";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                comboBox.addItem(rs.getString("name"));
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load products into combo box
    private void loadProductsCombo(JComboBox<String> comboBox) {
        comboBox.removeAllItems();

        try {
            String query = "SELECT name FROM products ORDER BY name";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                comboBox.addItem(rs.getString("name"));
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load customers into combo box
    private void loadCustomersCombo(JComboBox<String> comboBox) {
        comboBox.removeAllItems();

        try {
            String query = "SELECT name FROM customers ORDER BY name";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                comboBox.addItem(rs.getString("name"));
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper methods

    // Get seller ID by name
    private int getSellerIdByName(String name) throws SQLException {
        String query = "SELECT id FROM sellers WHERE name = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, name);

        ResultSet rs = pstmt.executeQuery();
        int id = -1;

        if (rs.next()) {
            id = rs.getInt("id");
        }

        rs.close();
        pstmt.close();

        return id;
    }

    // Get customer ID by name
    private int getCustomerIdByName(String name) throws SQLException {
        String query = "SELECT id FROM customers WHERE name = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, name);

        ResultSet rs = pstmt.executeQuery();
        int id = -1;

        if (rs.next()) {
            id = rs.getInt("id");
        }

        rs.close();
        pstmt.close();

        return id;
    }

    // Get product ID by name
    private int getProductIdByName(String name) throws SQLException {
        String query = "SELECT id FROM products WHERE name = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, name);

        ResultSet rs = pstmt.executeQuery();
        int id = -1;

        if (rs.next()) {
            id = rs.getInt("id");
        }

        rs.close();
        pstmt.close();

        return id;
    }

    // Get product start price
    private double getProductStartPrice(int productId) throws SQLException {
        String query = "SELECT start_price FROM products WHERE id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, productId);

        ResultSet rs = pstmt.executeQuery();
        double price = 0.0;

        if (rs.next()) {
            price = rs.getDouble("start_price");
        }

        rs.close();
        pstmt.close();

        return price;
    }

    // Check if product is in active auction
    private boolean isProductInActiveAuction(int productId) throws SQLException {
        String query = "SELECT COUNT(*) FROM auctions WHERE product_id = ? AND status = 'Active'";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, productId);

        ResultSet rs = pstmt.executeQuery();
        boolean result = false;

        if (rs.next()) {
            result = rs.getInt(1) > 0;
        }

        rs.close();
        pstmt.close();

        return result;
    }

    // Clear seller form
    private void clearSellerForm() {
        // This is implemented as a lambda in the createSellerPanel method
        // Just a placeholder method for organization
    }

    // Clear customer form
    private void clearCustomerForm() {
        // This is implemented as a lambda in the createCustomerPanel method
        // Just a placeholder method for organization
    }

    // Clear product form
    private void clearProductForm() {
        // This is implemented as a lambda in the createProductPanel method
        // Just a placeholder method for organization
    }

    // Clear auction form
    private void clearAuctionForm() {
        // This is implemented as a lambda in the createAuctionPanel method
        // Just a placeholder method for organization
    }

    // Main method
    public static void main(String[] args) {
        // Set look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Use nicer looking fonts
            Font defaultFont = new Font("Segoe UI", Font.PLAIN, 12);
            UIManager.put("Button.font", defaultFont);
            UIManager.put("Label.font", defaultFont);
            UIManager.put("TextField.font", defaultFont);
            UIManager.put("ComboBox.font", defaultFont);
            UIManager.put("Table.font", defaultFont);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create and display the application
        SwingUtilities.invokeLater(() -> new Main());
    }
}
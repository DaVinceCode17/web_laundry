package com.resources.dao;

import com.resources.model.Order;
import com.resources.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    
    public Order saveToSetPricing(Order order) throws SQLException {
        String sql = "INSERT INTO set_pricing_orders (customer_id, services, service_type, queue_number, weight, price, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, order.getCustomerId());
            pstmt.setString(2, order.getServices());
            pstmt.setString(3, order.getServiceType());
            pstmt.setInt(4, order.getQueueNumber());
            pstmt.setDouble(5, order.getWeight());
            pstmt.setDouble(6, order.getPrice());
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) order.setId(rs.getInt(1));
            }
        }
        
        String logSql = "INSERT INTO laundry_logs (customer_id, services, service_type, queue_number, weight, price, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(logSql)) {
            pstmt.setInt(1, order.getCustomerId());
            pstmt.setString(2, order.getServices());
            pstmt.setString(3, order.getServiceType());
            pstmt.setInt(4, order.getQueueNumber());
            pstmt.setDouble(5, order.getWeight());
            pstmt.setDouble(6, order.getPrice());
            pstmt.setString(7, "set_pricing");
            pstmt.executeUpdate();
        }
        
        return order;
    }
    
    // ============================================================
    // MOVE METHODS USING TABLE ID + CUSTOMER_ID
    // ============================================================
    
    public Order moveToPending(int id, int customerId) throws SQLException {
        return moveOrderById(id, customerId, "set_pricing_orders", "pending_orders", "pending");
    }
    
    public Order moveToWash(int id, int customerId) throws SQLException {
        return moveOrderById(id, customerId, "pending_orders", "to_wash_orders", "to_wash");
    }
    
    public Order moveToDry(int id, int customerId) throws SQLException {
        return moveOrderById(id, customerId, "to_wash_orders", "to_dry_orders", "to_dry");
    }
    
    public Order moveToIron(int id, int customerId) throws SQLException {
        return moveOrderById(id, customerId, "to_dry_orders", "to_iron_orders", "to_iron");
    }
    
    public Order moveToFold(int id, int customerId) throws SQLException {
        return moveOrderById(id, customerId, "to_iron_orders", "to_fold_orders", "to_fold");
    }
    
    public Order moveToForPickup(int id, int customerId) throws SQLException {
        return moveOrderById(id, customerId, "to_fold_orders", "for_pickup_orders", "for_pickup");
    }
    
    public Order moveToDeliver(int id, int customerId) throws SQLException {
        return moveOrderById(id, customerId, "to_fold_orders", "to_deliver_orders", "to_deliver");
    }
    
    public Order moveToClaimed(int id, int customerId) throws SQLException {
        return moveOrderById(id, customerId, "to_deliver_orders", "claimed_orders", "claimed");
    }
    
    public Order moveToClaimedFromPickup(int id, int customerId) throws SQLException {
        return moveOrderById(id, customerId, "for_pickup_orders", "claimed_orders", "claimed");
    }
    
    // ============================================================
    // GENERIC MOVE ORDER USING TABLE ID + CUSTOMER_ID
    // ============================================================
    private Order moveOrderById(int id, int customerId, String fromTable, String toTable, String newStatus) throws SQLException {
        // SELECT - Hanapin ang order gamit ang ID + Customer ID
        String selectSql = "SELECT o.*, c.first_name, c.last_name, c.address FROM " + fromTable + " o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.id = ? AND o.customer_id = ?";
        Order order = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, customerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                order = mapWithName(rs, newStatus);
            }
        }
        if (order == null) return null;
        
        // INSERT sa bagong table
        String insertSql = "INSERT INTO " + toTable + " (customer_id, services, service_type, queue_number, weight, price, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        if (toTable.equals("to_deliver_orders")) {
            insertSql = "INSERT INTO to_deliver_orders (customer_id, address, services, service_type, queue_number, weight, price, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, order.getCustomerId());
                pstmt.setString(2, order.getCustomerAddress());
                pstmt.setString(3, order.getServices());
                pstmt.setString(4, order.getServiceType());
                pstmt.setInt(5, order.getQueueNumber());
                pstmt.setDouble(6, order.getWeight());
                pstmt.setDouble(7, order.getPrice());
                pstmt.setString(8, order.getCreatedAt());
                pstmt.executeUpdate();
            }
        } else {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, order.getCustomerId());
                pstmt.setString(2, order.getServices());
                pstmt.setString(3, order.getServiceType());
                pstmt.setInt(4, order.getQueueNumber());
                pstmt.setDouble(5, order.getWeight());
                pstmt.setDouble(6, order.getPrice());
                pstmt.setString(7, order.getCreatedAt());
                pstmt.executeUpdate();
            }
        }
        
        // DELETE mula sa lumang table gamit ang ID + Customer ID
        String deleteSql = "DELETE FROM " + fromTable + " WHERE id = ? AND customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, customerId);
            pstmt.executeUpdate();
        }
        
        // UPDATE laundry_logs
        String updateLogSql = "UPDATE laundry_logs SET status = ? WHERE queue_number = ? AND customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateLogSql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, order.getQueueNumber());
            pstmt.setInt(3, customerId);
            pstmt.executeUpdate();
        }
        
        return order;
    }
    
    // ============================================================
    // UPDATE WEIGHT & PRICE USING TABLE ID + CUSTOMER_ID
    // ============================================================
    public boolean updateSetPricingWeightAndPrice(int id, int customerId, double weight, double price) throws SQLException {
        String sql = "UPDATE set_pricing_orders SET weight = ?, price = ? WHERE id = ? AND customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, weight);
            pstmt.setDouble(2, price);
            pstmt.setInt(3, id);
            pstmt.setInt(4, customerId);
            boolean updated = pstmt.executeUpdate() > 0;
            
            if (updated) {
                String logSql = "UPDATE laundry_logs SET weight = ?, price = ? WHERE queue_number = (SELECT queue_number FROM set_pricing_orders WHERE id = ?) AND customer_id = ?";
                try (Connection conn2 = DatabaseConnection.getConnection();
                     PreparedStatement pstmt2 = conn2.prepareStatement(logSql)) {
                    pstmt2.setDouble(1, weight);
                    pstmt2.setDouble(2, price);
                    pstmt2.setInt(3, id);
                    pstmt2.setInt(4, customerId);
                    pstmt2.executeUpdate();
                }
            }
            return updated;
        }
    }
    
    // ============================================================
    // GET ORDERS
    // ============================================================
    public List<Order> getSetPricingOrders() throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, c.first_name, c.last_name, c.address FROM set_pricing_orders o LEFT JOIN customers c ON o.customer_id = c.id ORDER BY o.id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapWithName(rs, "set_pricing"));
            }
        }
        return list;
    }
    
    public List<Order> getOrdersByTable(String tableName) throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, c.first_name, c.last_name, c.address FROM " + tableName + " o LEFT JOIN customers c ON o.customer_id = c.id ORDER BY o.id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String status = tableName.replace("_orders", "");
                list.add(mapWithName(rs, status));
            }
        }
        return list;
    }
    
    public List<Order> getLaundryLogs() throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, c.first_name, c.last_name, c.address FROM laundry_logs o LEFT JOIN customers c ON o.customer_id = c.id ORDER BY o.id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapWithName(rs, rs.getString("status")));
            }
        }
        return list;
    }
    
    public int getNextQueueNumber() throws SQLException {
        String sql = "SELECT COALESCE(MAX(queue_number), 0) + 1 FROM set_pricing_orders";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
            return 1;
        }
    }
    
    public List<Order> getOrdersByCustomer(int customerId) throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT 'set_pricing' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name, c.address FROM set_pricing_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'pending' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name, c.address FROM pending_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'to_wash' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name, c.address FROM to_wash_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'to_dry' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name, c.address FROM to_dry_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'to_iron' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name, c.address FROM to_iron_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'to_fold' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name, c.address FROM to_fold_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'for_pickup' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name, c.address FROM for_pickup_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'to_deliver' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name, c.address FROM to_deliver_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'claimed' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name, c.address FROM claimed_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 9; i++) pstmt.setInt(i, customerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapWithStatus(rs));
            }
        }
        return list;
    }
    
    private Order mapWithName(ResultSet rs, String status) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setCustomerId(rs.getInt("customer_id"));
        o.setServices(rs.getString("services"));
        o.setServiceType(rs.getString("service_type"));
        o.setQueueNumber(rs.getInt("queue_number"));
        o.setWeight(rs.getDouble("weight"));
        o.setPrice(rs.getDouble("price"));
        o.setCreatedAt(rs.getString("created_at"));
        o.setStatus(status);
        String fn = rs.getString("first_name");
        String ln = rs.getString("last_name");
        String address = rs.getString("address");
        if (fn != null && ln != null) o.setCustomerName(fn + " " + ln);
        if (address != null) o.setCustomerAddress(address);
        return o;
    }
    
    private Order mapWithStatus(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setCustomerId(rs.getInt("customer_id"));
        o.setServices(rs.getString("services"));
        o.setServiceType(rs.getString("service_type"));
        o.setQueueNumber(rs.getInt("queue_number"));
        o.setWeight(rs.getDouble("weight"));
        o.setPrice(rs.getDouble("price"));
        o.setCreatedAt(rs.getString("created_at"));
        o.setStatus(rs.getString("status"));
        String fn = rs.getString("first_name");
        String ln = rs.getString("last_name");
        String address = rs.getString("address");
        if (fn != null && ln != null) o.setCustomerName(fn + " " + ln);
        if (address != null) o.setCustomerAddress(address);
        return o;
    }
}
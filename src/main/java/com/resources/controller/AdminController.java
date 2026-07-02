package com.resources.controller;

import com.resources.model.Order;
import com.resources.model.Pricing;
import com.resources.service.LaundryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/admin/*")
public class AdminController extends HttpServlet {
    
    private LaundryService service;
    
    @Override
    public void init() {
        service = new LaundryService();
    }
    
    private void getOrdersByTable(String tableName, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            List<Order> orders = service.getOrdersByTable(tableName);
            out.write(toJsonOrders(orders));
        } catch (Exception e) {
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }
    
    private void getSetPricingOrders(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            List<Order> orders = service.getSetPricingOrders();
            out.write(toJsonOrders(orders));
        } catch (Exception e) {
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }
    
    private void getLaundryLogs(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            List<Order> orders = service.getLaundryLogs();
            out.write(toJsonOrders(orders));
        } catch (Exception e) {
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }
    
    // ============================================================
    // MOVE ORDER USING TABLE ID + CUSTOMER_ID
    // ============================================================
    private void moveOrder(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        String json = readBody(req);
        
        try {
            int id = Integer.parseInt(extract(json, "id"));
            int customerId = Integer.parseInt(extract(json, "customerId"));
            String action = extract(json, "action");
            
            Order result = null;
            switch (action) {
                case "to_pending": result = service.moveToPending(id, customerId); break;
                case "to_wash": result = service.moveToWash(id, customerId); break;
                case "to_dry": result = service.moveToDry(id, customerId); break;
                case "to_iron": result = service.moveToIron(id, customerId); break;
                case "to_fold": result = service.moveToFold(id, customerId); break;
                case "for_pickup": result = service.moveToForPickup(id, customerId); break;
                case "to_deliver": result = service.moveToDeliver(id, customerId); break;
                case "claimed": result = service.moveToClaimed(id, customerId); break;
                case "claimed_from_pickup": result = service.moveToClaimedFromPickup(id, customerId); break;
                default: result = null;
            }
            
            if (result != null) {
                out.write("{\"success\":true,\"order\":{\"id\":" + result.getId() + ",\"queueNumber\":" + result.getQueueNumber() + ",\"status\":\"" + action + "\"}}");
            } else {
                out.write("{\"success\":false,\"message\":\"Order not found\"}");
            }
        } catch (Exception e) {
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }
    
    // ============================================================
    // UPDATE WEIGHT & PRICE USING TABLE ID + CUSTOMER_ID
    // ============================================================
    private void updateSetPricingWeightAndPrice(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        String json = readBody(req);
        
        try {
            int id = Integer.parseInt(extract(json, "id"));
            int customerId = Integer.parseInt(extract(json, "customerId"));
            double weight = Double.parseDouble(extract(json, "weight"));
            double price = Double.parseDouble(extract(json, "price"));
            
            boolean success = service.updateSetPricingWeightAndPrice(id, customerId, weight, price);
            if (success) {
                out.write("{\"success\":true,\"order\":{\"id\":" + id + ",\"weight\":" + weight + ",\"price\":" + price + "}}");
            } else {
                out.write("{\"success\":false,\"message\":\"Update failed\"}");
            }
        } catch (Exception e) {
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }
    
    private void getPricing(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Pricing p = service.getPricing();
            out.write("{\"success\":true,\"pricing\":{\"id\":" + p.getId() + ",\"washPrice\":" + p.getWashPrice() + ",\"dryPrice\":" + p.getDryPrice() + ",\"ironPrice\":" + p.getIronPrice() + ",\"foldPrice\":" + p.getFoldPrice() + "}}");
        } catch (Exception e) {
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }
    
    private void updatePricing(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        String json = readBody(req);
        
        try {
            Pricing p = new Pricing();
            p.setId(Integer.parseInt(extract(json, "id")));
            p.setWashPrice(Double.parseDouble(extract(json, "washPrice")));
            p.setDryPrice(Double.parseDouble(extract(json, "dryPrice")));
            p.setIronPrice(Double.parseDouble(extract(json, "ironPrice")));
            p.setFoldPrice(Double.parseDouble(extract(json, "foldPrice")));
            boolean success = service.updatePricing(p);
            out.write("{\"success\":" + success + ",\"message\":\"" + (success ? "Pricing updated" : "Update failed") + "\"}");
        } catch (Exception e) {
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        
        try {
            if ("/pricing".equals(path)) {
                getPricing(resp);
            } else if ("/set-pricing".equals(path)) {
                getSetPricingOrders(resp);
            } else if ("/pending".equals(path)) {
                getOrdersByTable("pending_orders", resp);
            } else if ("/to_wash".equals(path)) {
                getOrdersByTable("to_wash_orders", resp);
            } else if ("/to_dry".equals(path)) {
                getOrdersByTable("to_dry_orders", resp);
            } else if ("/to_iron".equals(path)) {
                getOrdersByTable("to_iron_orders", resp);
            } else if ("/to_fold".equals(path)) {
                getOrdersByTable("to_fold_orders", resp);
            } else if ("/for_pickup".equals(path)) {
                getOrdersByTable("for_pickup_orders", resp);
            } else if ("/to_deliver".equals(path)) {
                getOrdersByTable("to_deliver_orders", resp);
            } else if ("/claimed".equals(path)) {
                getOrdersByTable("claimed_orders", resp);
            } else if ("/laundry_logs".equals(path)) {
                getLaundryLogs(resp);
            } else {
                out.write("{\"success\":false,\"message\":\"Invalid endpoint: " + path + "\"}");
            }
        } catch (Exception e) {
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if ("/move".equals(path)) {
            moveOrder(req, resp);
        } else if ("/update-weight-price".equals(path)) {
            updateSetPricingWeightAndPrice(req, resp);
        } else {
            resp.setContentType("application/json");
            resp.getWriter().write("{\"success\":false,\"message\":\"Invalid endpoint\"}");
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if ("/pricing".equals(path)) {
            updatePricing(req, resp);
        } else {
            resp.setContentType("application/json");
            resp.getWriter().write("{\"success\":false,\"message\":\"Invalid endpoint\"}");
        }
    }
    
    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
    
    private String extract(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(start, end).trim();
    }
    
    private String toJsonOrders(List<Order> orders) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true,\"orders\":[");
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            sb.append("{");
            sb.append("\"id\":").append(o.getId()).append(",");
            sb.append("\"queueNumber\":").append(o.getQueueNumber()).append(",");
            sb.append("\"customerId\":").append(o.getCustomerId()).append(",");
            sb.append("\"customerName\":\"").append(escape(o.getCustomerName())).append("\",");
            sb.append("\"customerAddress\":\"").append(escape(o.getCustomerAddress())).append("\",");
            sb.append("\"services\":\"").append(escape(o.getServices())).append("\",");
            sb.append("\"status\":\"").append(escape(o.getStatus())).append("\",");
            sb.append("\"serviceType\":\"").append(escape(o.getServiceType())).append("\",");
            double weight = o.getWeight();
            double price = o.getPrice();
            sb.append("\"weight\":").append(weight > 0 ? weight : 0).append(",");
            sb.append("\"price\":").append(price > 0 ? price : 0).append(",");
            sb.append("\"createdAt\":\"").append(escape(o.getCreatedAt())).append("\"");
            sb.append("}");
            if (i < orders.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }
    
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }
}
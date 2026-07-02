package com.resources.service;

import com.resources.dao.CustomerDAO;
import com.resources.dao.OrderDAO;
import com.resources.dao.PricingDAO;
import com.resources.model.Customer;
import com.resources.model.Order;
import com.resources.model.Pricing;
import java.sql.SQLException;
import java.util.List;

public class LaundryService {
    
    private CustomerDAO customerDAO;
    private OrderDAO orderDAO;
    private PricingDAO pricingDAO;
    
    public LaundryService() {
        customerDAO = new CustomerDAO();
        orderDAO = new OrderDAO();
        pricingDAO = new PricingDAO();
    }
    
    public Customer login(String contact, String password) throws SQLException {
        return customerDAO.findByContactAndPassword(contact, password);
    }
    
    public boolean register(Customer customer) throws SQLException {
        if (customerDAO.findByContact(customer.getContact()) != null) {
            throw new IllegalArgumentException("Contact already registered");
        }
        return customerDAO.save(customer);
    }
    
    public List<Customer> getAllCustomers() throws SQLException {
        return customerDAO.findAll();
    }
    
    public Customer getCustomerById(int id) throws SQLException {
        return customerDAO.findById(id);
    }
    
    public Customer getCustomerByContact(String contact) throws SQLException {
        return customerDAO.findByContact(contact);
    }
    
    public Order createOrder(Order order) throws SQLException {
        order.setQueueNumber(orderDAO.getNextQueueNumber());
        order.setStatus("set_pricing");
        return orderDAO.saveToSetPricing(order);
    }
    
    public boolean updateSetPricingWeightAndPrice(int id, int customerId, double weight, double price) throws SQLException {
        return orderDAO.updateSetPricingWeightAndPrice(id, customerId, weight, price);
    }
    
    public Order moveToPending(int id, int customerId) throws SQLException {
        return orderDAO.moveToPending(id, customerId);
    }
    
    public Order moveToWash(int id, int customerId) throws SQLException {
        return orderDAO.moveToWash(id, customerId);
    }
    
    public Order moveToDry(int id, int customerId) throws SQLException {
        return orderDAO.moveToDry(id, customerId);
    }
    
    public Order moveToIron(int id, int customerId) throws SQLException {
        return orderDAO.moveToIron(id, customerId);
    }
    
    public Order moveToFold(int id, int customerId) throws SQLException {
        return orderDAO.moveToFold(id, customerId);
    }
    
    public Order moveToForPickup(int id, int customerId) throws SQLException {
        return orderDAO.moveToForPickup(id, customerId);
    }
    
    public Order moveToDeliver(int id, int customerId) throws SQLException {
        return orderDAO.moveToDeliver(id, customerId);
    }
    
    public Order moveToClaimed(int id, int customerId) throws SQLException {
        return orderDAO.moveToClaimed(id, customerId);
    }
    
    public Order moveToClaimedFromPickup(int id, int customerId) throws SQLException {
        return orderDAO.moveToClaimedFromPickup(id, customerId);
    }
    
    public List<Order> getSetPricingOrders() throws SQLException {
        return orderDAO.getSetPricingOrders();
    }
    
    public List<Order> getOrdersByTable(String tableName) throws SQLException {
        return orderDAO.getOrdersByTable(tableName);
    }
    
    public List<Order> getLaundryLogs() throws SQLException {
        return orderDAO.getLaundryLogs();
    }
    
    public List<Order> getOrdersByCustomer(int customerId) throws SQLException {
        return orderDAO.getOrdersByCustomer(customerId);
    }
    
    public Pricing getPricing() throws SQLException {
        Pricing p = pricingDAO.getPricing();
        if (p == null) {
            pricingDAO.insertDefaultPricing();
            p = pricingDAO.getPricing();
        }
        return p;
    }
    
    public boolean updatePricing(Pricing pricing) throws SQLException {
        return pricingDAO.updatePricing(pricing);
    }
}
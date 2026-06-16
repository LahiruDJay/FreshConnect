package com.s23010691.freshconnect.models;

import java.io.Serializable;

/*
 * Product Model
 * Represents an agricultural product listed in the application with its details, location, and images.
 */
public class Product implements Serializable {
    public String id;
    public String name;
    public String category;
    public double price;
    public String unit;
    public String quantity;
    public String location_name;
    public double latitude;
    public double longitude;
    public String description;
    public String[] image_urls;
    public String user_id;
    public String created_at;

    /*
     * Constructor to initialize a new Product instance with all required details.
     * Parameters:
     *   - name: The name of the product.
     *   - category: The category the product belongs to.
     *   - price: The price of the product.
     *   - unit: The measurement unit (e.g., kg, liters).
     *   - quantity: The available quantity of the product.
     *   - location_name: The textual location name.
     *   - latitude: The geographical latitude of the product's location.
     *   - longitude: The geographical longitude of the product's location.
     *   - description: A detailed description of the product.
     *   - image_urls: An array of URLs for the product's images.
     *   - user_id: The ID of the user who listed the product.
     */
    public Product(String name, String category, double price, String unit, String quantity, 
                   String location_name, double latitude, double longitude, 
                   String description, String[] image_urls, String user_id) {
        // Assign parameters to class fields
        this.name = name;
        this.category = category;
        this.price = price;
        this.unit = unit;
        this.quantity = quantity;
        this.location_name = location_name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.image_urls = image_urls;
        this.user_id = user_id;
    }

    /*
     * Compares this Product instance with another object to check for equality.
     * Parameters:
     *   - obj: The object to compare with.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product other = (Product) obj;
        if (this.id != null && other.id != null) {
            return this.id.equals(other.id);
        }
        return (this.name != null && this.name.equals(other.name))
                && (this.user_id != null && this.user_id.equals(other.user_id))
                && (this.created_at != null && this.created_at.equals(other.created_at));
    }

    /*
     * Computes a hash code for this Product instance based on its key fields.
     */
    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (user_id != null ? user_id.hashCode() : 0);
        result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
        return result;
    }
}


-- =============================================================================
-- Performance Indexes for NVT Project - Warehouse Management System
-- =============================================================================
-- Run this script manually in PostgreSQL to create optimized indexes
-- This improves performance under high concurrent load (100-1000+ users)
-- =============================================================================

-- Enable the pg_trgm extension for pattern matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =============================================================================
-- VEHICLE INDEXES (from original file)
-- =============================================================================

-- Functional indexes with LOWER() for case-insensitive exact/prefix matching
CREATE INDEX IF NOT EXISTS idx_vehicle_license_plate_lower ON vehicles (LOWER(license_plate));
CREATE INDEX IF NOT EXISTS idx_vehicle_brand_name_lower ON vehicle_brands (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_vehicle_model_name_lower ON vehicle_models (LOWER(name));

-- GIN trigram indexes for LIKE '%pattern%' searches
CREATE INDEX IF NOT EXISTS idx_vehicle_license_plate_trgm ON vehicles USING GIN (license_plate gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_vehicle_brand_name_trgm ON vehicle_brands USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_vehicle_model_name_trgm ON vehicle_models USING GIN (name gin_trgm_ops);

-- Vehicle foreign key indexes
CREATE INDEX IF NOT EXISTS idx_vehicle_brand_id ON vehicles (brand_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_model_id ON vehicles (model_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_model_brand_id ON vehicle_models (brand_id);

-- =============================================================================
-- USER AND AUTH INDEXES
-- =============================================================================

-- User table indexes for authentication (critical for JWT filter performance)
CREATE INDEX IF NOT EXISTS idx_user_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_user_username_lower ON users (LOWER(username));
CREATE INDEX IF NOT EXISTS idx_user_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_user_active ON users (active);

-- Token table indexes
CREATE INDEX IF NOT EXISTS idx_token_access_token ON token (access_token);
CREATE INDEX IF NOT EXISTS idx_token_refresh_token ON token (refresh_token);
CREATE INDEX IF NOT EXISTS idx_token_user_id ON token (user_id);

-- =============================================================================
-- WAREHOUSE INDEXES - Critical for Performance
-- =============================================================================

-- GIN Trigram indexes for warehouse search (supports ILIKE '%pattern%')
CREATE INDEX IF NOT EXISTS idx_warehouse_name_trgm ON warehouses USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_warehouse_street_trgm ON warehouses USING GIN (street gin_trgm_ops);

-- B-tree indexes for warehouse lookups and joins
CREATE INDEX IF NOT EXISTS idx_warehouse_country_id ON warehouses (country_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_city_id ON warehouses (city_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_active ON warehouses (active);
CREATE INDEX IF NOT EXISTS idx_warehouse_online ON warehouses (online);
CREATE INDEX IF NOT EXISTS idx_warehouse_created_at ON warehouses (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_warehouse_active_online ON warehouses (active, online);

-- Warehouse sector indexes
CREATE INDEX IF NOT EXISTS idx_warehouse_sector_warehouse_id ON warehouse_sectors (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_sector_name_trgm ON warehouse_sectors USING GIN (name gin_trgm_ops);

-- Warehouse image indexes
CREATE INDEX IF NOT EXISTS idx_warehouse_image_warehouse_id ON warehouse_images (warehouse_id);

-- =============================================================================
-- LOCATION INDEXES (Country/City)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_country_name_trgm ON countries USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_city_name_trgm ON cities USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_city_country_id ON cities (country_id);

-- =============================================================================
-- PRODUCT INDEXES
-- =============================================================================

-- GIN Trigram indexes for product search
CREATE INDEX IF NOT EXISTS idx_product_name_trgm ON products USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_product_sku_trgm ON products USING GIN (sku gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_product_description_trgm ON products USING GIN (description gin_trgm_ops);

-- B-tree indexes for product queries
CREATE INDEX IF NOT EXISTS idx_product_category ON products (category);
CREATE INDEX IF NOT EXISTS idx_product_price ON products (price);
CREATE INDEX IF NOT EXISTS idx_product_active ON products (active);
CREATE INDEX IF NOT EXISTS idx_product_active_category ON products (active, category);
CREATE INDEX IF NOT EXISTS idx_product_active_price ON products (active, price);

-- Product image indexes
CREATE INDEX IF NOT EXISTS idx_product_image_product_id ON product_images (product_id);

-- =============================================================================
-- INVENTORY INDEXES - Critical for product availability
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory (product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warehouse_id ON inventory (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_sector_id ON inventory (sector_id);
CREATE INDEX IF NOT EXISTS idx_inventory_product_warehouse ON inventory (product_id, warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_quantity ON inventory (quantity, reserved_quantity);

-- =============================================================================
-- ORDER INDEXES
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_order_customer_id ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_order_company_id ON orders (company_id);
CREATE INDEX IF NOT EXISTS idx_order_created_at ON orders (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_order_number ON orders (order_number);
CREATE INDEX IF NOT EXISTS idx_order_customer_created ON orders (customer_id, created_at DESC);

-- Order item indexes
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product_id ON order_items (product_id);
CREATE INDEX IF NOT EXISTS idx_order_item_warehouse_id ON order_items (warehouse_id);

-- =============================================================================
-- COMPANY AND REGISTRATION REQUEST INDEXES
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_company_name_trgm ON companies USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_company_country_id ON companies (country_id);
CREATE INDEX IF NOT EXISTS idx_company_city_id ON companies (city_id);

CREATE INDEX IF NOT EXISTS idx_registration_request_company_id ON registration_requests (company_id);
CREATE INDEX IF NOT EXISTS idx_registration_request_customer_id ON registration_requests (customer_id);
CREATE INDEX IF NOT EXISTS idx_registration_request_status ON registration_requests (status);
CREATE INDEX IF NOT EXISTS idx_registration_request_created_at ON registration_requests (created_at DESC);

-- =============================================================================
-- Update table statistics for query planner
-- =============================================================================

ANALYZE vehicles;
ANALYZE vehicle_brands;
ANALYZE vehicle_models;
ANALYZE users;
ANALYZE token;
ANALYZE warehouses;
ANALYZE warehouse_sectors;
ANALYZE warehouse_images;
ANALYZE countries;
ANALYZE cities;
ANALYZE products;
ANALYZE product_images;
ANALYZE inventory;
ANALYZE orders;
ANALYZE order_items;
ANALYZE companies;
ANALYZE registration_requests;

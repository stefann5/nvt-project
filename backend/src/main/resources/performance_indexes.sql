-- =============================================================================
-- Performance Indexes for NVT Project - Warehouse Management System
-- =============================================================================
-- This script creates optimized indexes for all frequently queried tables
-- to improve performance under high concurrent load (100-1000+ users)
-- =============================================================================

-- Step 1: Enable the pg_trgm extension for fast text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =============================================================================
-- USER INDEXES - CRITICAL FOR AUTHENTICATION (runs on EVERY request!)
-- =============================================================================

-- MOST IMPORTANT INDEX - username lookup for JWT authentication
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username 
ON users (username);

-- Index for user activation token lookups
CREATE INDEX IF NOT EXISTS idx_users_activation_token 
ON users (activation_token) WHERE activation_token IS NOT NULL;

-- Index for active users
CREATE INDEX IF NOT EXISTS idx_users_active 
ON users (active);

-- Composite index for login queries
CREATE INDEX IF NOT EXISTS idx_users_username_active 
ON users (username, active);

-- =============================================================================
-- WAREHOUSE INDEXES - Most Critical for Performance
-- =============================================================================

-- GIN Trigram indexes for warehouse search (supports ILIKE '%pattern%')
CREATE INDEX IF NOT EXISTS idx_warehouse_name_trgm 
ON warehouses USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_warehouse_street_trgm 
ON warehouses USING GIN (street gin_trgm_ops);

-- Functional indexes for LOWER() function queries
CREATE INDEX IF NOT EXISTS idx_warehouse_name_lower 
ON warehouses (LOWER(name));

CREATE INDEX IF NOT EXISTS idx_warehouse_street_lower 
ON warehouses (LOWER(street));

-- B-tree indexes for warehouse lookups and joins
CREATE INDEX IF NOT EXISTS idx_warehouse_country_id 
ON warehouses (country_id);

CREATE INDEX IF NOT EXISTS idx_warehouse_city_id 
ON warehouses (city_id);

CREATE INDEX IF NOT EXISTS idx_warehouse_active 
ON warehouses (active);

CREATE INDEX IF NOT EXISTS idx_warehouse_online 
ON warehouses (online);

CREATE INDEX IF NOT EXISTS idx_warehouse_created_at 
ON warehouses (created_at DESC);

-- Composite index for common warehouse queries
CREATE INDEX IF NOT EXISTS idx_warehouse_active_online 
ON warehouses (active, online);

-- =============================================================================
-- WAREHOUSE SECTOR INDEXES
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_warehouse_sector_warehouse_id 
ON warehouse_sectors (warehouse_id);

CREATE INDEX IF NOT EXISTS idx_warehouse_sector_name_trgm 
ON warehouse_sectors USING GIN (name gin_trgm_ops);

-- =============================================================================
-- WAREHOUSE IMAGE INDEXES
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_warehouse_image_warehouse_id 
ON warehouse_images (warehouse_id);

-- =============================================================================
-- COUNTRY AND CITY INDEXES (for warehouse search joins)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_country_name_trgm 
ON countries USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_country_name_lower 
ON countries (LOWER(name));

CREATE INDEX IF NOT EXISTS idx_city_name_trgm 
ON cities USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_city_name_lower 
ON cities (LOWER(name));

CREATE INDEX IF NOT EXISTS idx_city_country_id 
ON cities (country_id);

-- =============================================================================
-- PRODUCT INDEXES
-- =============================================================================

-- GIN Trigram indexes for product search
CREATE INDEX IF NOT EXISTS idx_product_name_trgm 
ON products USING GIN (name gin_trgm_ops);

-- Functional indexes for LOWER() function queries  
CREATE INDEX IF NOT EXISTS idx_product_name_lower 
ON products (LOWER(name));

CREATE INDEX IF NOT EXISTS idx_product_sku_lower 
ON products (LOWER(sku));

CREATE INDEX IF NOT EXISTS idx_product_description_lower 
ON products (LOWER(description));

CREATE INDEX IF NOT EXISTS idx_product_sku_trgm 
ON products USING GIN (sku gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_product_description_trgm 
ON products USING GIN (description gin_trgm_ops);

-- B-tree indexes for product queries
CREATE INDEX IF NOT EXISTS idx_product_category 
ON products (category);

CREATE INDEX IF NOT EXISTS idx_product_price 
ON products (price);

CREATE INDEX IF NOT EXISTS idx_product_active 
ON products (active);

-- Composite index for product search with filters
CREATE INDEX IF NOT EXISTS idx_product_active_category 
ON products (active, category);

CREATE INDEX IF NOT EXISTS idx_product_active_price 
ON products (active, price);

-- =============================================================================
-- PRODUCT IMAGE INDEXES
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_product_image_product_id 
ON product_images (product_id);

-- =============================================================================
-- INVENTORY INDEXES - Critical for product availability queries
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_inventory_product_id 
ON inventory (product_id);

CREATE INDEX IF NOT EXISTS idx_inventory_warehouse_id 
ON inventory (warehouse_id);

CREATE INDEX IF NOT EXISTS idx_inventory_sector_id 
ON inventory (sector_id);

-- Composite index for inventory lookups
CREATE INDEX IF NOT EXISTS idx_inventory_product_warehouse 
ON inventory (product_id, warehouse_id);

-- Index for available quantity calculations
CREATE INDEX IF NOT EXISTS idx_inventory_quantity 
ON inventory (quantity, reserved_quantity);

-- =============================================================================
-- ORDER INDEXES
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_order_customer_id 
ON orders (customer_id);

CREATE INDEX IF NOT EXISTS idx_order_company_id 
ON orders (company_id);

CREATE INDEX IF NOT EXISTS idx_order_created_at 
ON orders (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_order_status 
ON orders (status);

CREATE INDEX IF NOT EXISTS idx_order_number 
ON orders (order_number);

-- Composite index for customer order queries
CREATE INDEX IF NOT EXISTS idx_order_customer_created 
ON orders (customer_id, created_at DESC);

-- =============================================================================
-- ORDER ITEM INDEXES
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_order_item_order_id 
ON order_items (order_id);

CREATE INDEX IF NOT EXISTS idx_order_item_product_id 
ON order_items (product_id);

CREATE INDEX IF NOT EXISTS idx_order_item_warehouse_id 
ON order_items (warehouse_id);

-- =============================================================================
-- COMPANY INDEXES (for registration requests)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_company_name_trgm 
ON companies USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_company_country_id 
ON companies (country_id);

CREATE INDEX IF NOT EXISTS idx_company_city_id 
ON companies (city_id);

-- =============================================================================
-- REGISTRATION REQUEST INDEXES
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_registration_request_company_id 
ON registration_requests (company_id);

CREATE INDEX IF NOT EXISTS idx_registration_request_customer_id 
ON registration_requests (customer_id);

CREATE INDEX IF NOT EXISTS idx_registration_request_status 
ON registration_requests (status);

CREATE INDEX IF NOT EXISTS idx_registration_request_created_at 
ON registration_requests (created_at DESC);

-- =============================================================================
-- USER INDEXES
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_user_username 
ON users (username);

CREATE INDEX IF NOT EXISTS idx_user_role 
ON users (role);

CREATE INDEX IF NOT EXISTS idx_user_active 
ON users (active);

-- =============================================================================
-- VEHICLE INDEXES (from testo project)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_vehicle_license_plate_trgm 
ON vehicles USING GIN (license_plate gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_vehicle_brand_name_trgm 
ON vehicle_brands USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_vehicle_model_name_trgm 
ON vehicle_models USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_vehicle_brand_id 
ON vehicles (brand_id);

CREATE INDEX IF NOT EXISTS idx_vehicle_model_id 
ON vehicles (model_id);

CREATE INDEX IF NOT EXISTS idx_vehicle_model_brand_id 
ON vehicle_models (brand_id);

-- =============================================================================
-- Step 2: Update table statistics for the query planner
-- =============================================================================

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
ANALYZE users;
ANALYZE vehicles;
ANALYZE vehicle_brands;
ANALYZE vehicle_models;

-- =============================================================================
-- Verify indexes were created
-- =============================================================================

SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE schemaname = 'public'
AND (
    indexname LIKE 'idx_%'
)
ORDER BY tablename, indexname;

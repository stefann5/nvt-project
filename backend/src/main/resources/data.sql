-- -- 1. Admin korisnik
-- INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
-- VALUES (1, 'admin', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Marko', 'Petrović', 'TechCorp', 2, 'ADMIN,USER', 'Admin', true, null, null);
--
-- -- 2. Regularni korisnik
-- INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
-- VALUES (2, 'john', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'John', 'Doe', 'MusicInc', 0, 'USER', 'User', true, null, null);
--
-- -- 3. Premium korisnik
-- INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
-- VALUES (3, 'ana.milic', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Ana', 'Milić', 'SoundWave', 0, 'USER,PREMIUM', 'PremiumUser', true, null, null);
--
-- -- 4. Moderator
-- INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
-- VALUES (4, 'stefan.mod', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Stefan', 'Nikolić', 'MusicPlatform', 1, 'MODERATOR,USER', 'Moderator', true, null, null);
--
-- -- 5. Test korisnik
-- INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
-- VALUES (5, 'test.user', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Test', 'Testović', 'TestOrg', 1, 'USER', 'User', true, null, null);
--
-- UPDATE public.id_generator
-- SET next_val = 300
-- WHERE sequence_name = 'user';

-- Create id_generator table if it doesn't exist
CREATE TABLE IF NOT EXISTS id_generator (
                                            sequence_name VARCHAR(255) PRIMARY KEY,
    next_val BIGINT NOT NULL
    );

-- Initialize sequences
INSERT INTO id_generator (sequence_name, next_val) VALUES ('users', 300) ON CONFLICT (sequence_name) DO NOTHING;
INSERT INTO id_generator (sequence_name, next_val) VALUES ('registration_requests', 300) ON CONFLICT (sequence_name) DO NOTHING;
INSERT INTO id_generator (sequence_name, next_val) VALUES ('orders', 300) ON CONFLICT (sequence_name) DO NOTHING;
INSERT INTO id_generator (sequence_name, next_val) VALUES ('order_items', 300) ON CONFLICT (sequence_name) DO NOTHING;
INSERT INTO id_generator (sequence_name, next_val) VALUES ('warehouses', 300) ON CONFLICT (sequence_name) DO NOTHING;
INSERT INTO id_generator (sequence_name, next_val) VALUES ('warehouse_sectors', 300) ON CONFLICT (sequence_name) DO NOTHING;

-- 1. ADMIN
INSERT INTO users (id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type) VALUES (1, 'Admin', 'Admin', 'admin','$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',2, 'ADMIN', true, NULL, NULL, 'Manager') ON CONFLICT (id) DO NOTHING;
-- 2. CA korisnik 1
INSERT INTO users (id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type) VALUES (2, 'Goran', 'Bijelic', 'tetak','$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',1, 'MANAGER', true, NULL, NULL, 'Manager')ON CONFLICT (id) DO NOTHING;
-- 3. CA korisnik 2
INSERT INTO users (id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type) VALUES (3, 'Miki', 'Bijelic', 'miki','$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',0, 'CUSTOMER', true, NULL, NULL, 'Customer')ON CONFLICT (id) DO NOTHING;
-- 4. Običan korisnik
INSERT INTO users (id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type) VALUES (4, 'Stefan', 'Nikolić', 'user@company.com','$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',0, 'CUSTOMER', true, NULL, NULL, 'Customer')ON CONFLICT (id) DO NOTHING;
-- 5. Test korisnik
INSERT INTO users (id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type) VALUES (5, 'Test', 'Testović', 'test@test.com','$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',0, 'CUSTOMER', true, NULL, NULL, 'Customer')ON CONFLICT (id) DO NOTHING;

-- Countries
INSERT INTO countries (id, name) VALUES (1, 'Serbia');
INSERT INTO countries (id, name) VALUES (2, 'Croatia');
INSERT INTO countries (id, name) VALUES (3, 'Germany');
INSERT INTO countries (id, name) VALUES (4, 'Austria');
INSERT INTO countries (id, name) VALUES (5, 'Hungary');

-- Serbia cities
INSERT INTO cities (id, name, country_id) VALUES (1, 'Belgrade', 1);
INSERT INTO cities (id, name, country_id) VALUES (2, 'Novi Sad', 1);
INSERT INTO cities (id, name, country_id) VALUES (3, 'Niš', 1);
INSERT INTO cities (id, name, country_id) VALUES (4, 'Kragujevac', 1);
INSERT INTO cities (id, name, country_id) VALUES (5, 'Subotica', 1);

-- Croatia cities
INSERT INTO cities (id, name, country_id) VALUES (6, 'Zagreb', 2);
INSERT INTO cities (id, name, country_id) VALUES (7, 'Split', 2);
INSERT INTO cities (id, name, country_id) VALUES (8, 'Rijeka', 2);

-- Germany cities
INSERT INTO cities (id, name, country_id) VALUES (9, 'Berlin', 3);
INSERT INTO cities (id, name, country_id) VALUES (10, 'Munich', 3);
INSERT INTO cities (id, name, country_id) VALUES (11, 'Hamburg', 3);

-- Austria cities
INSERT INTO cities (id, name, country_id) VALUES (12, 'Vienna', 4);
INSERT INTO cities (id, name, country_id) VALUES (13, 'Salzburg', 4);

-- Hungary cities
INSERT INTO cities (id, name, country_id) VALUES (14, 'Budapest', 5);
INSERT INTO cities (id, name, country_id) VALUES (15, 'Debrecen', 5);

-- Vehicle Brands
INSERT INTO vehicle_brands (id, name) VALUES (1, 'Mercedes-Benz') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (2, 'Volkswagen') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (3, 'Ford') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (4, 'Iveco') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (5, 'MAN') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (6, 'Renault') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (7, 'Fiat') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (8, 'Peugeot') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (9, 'Citroen') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (10, 'Toyota') ON CONFLICT (id) DO NOTHING;

-- Mercedes-Benz models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (1, 'Sprinter', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (2, 'Vito', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (3, 'Citan', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (4, 'Actros', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (5, 'Atego', 1) ON CONFLICT (id) DO NOTHING;

-- Volkswagen models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (6, 'Crafter', 2) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (7, 'Transporter', 2) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (8, 'Caddy', 2) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (9, 'Amarok', 2) ON CONFLICT (id) DO NOTHING;

-- Ford models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (10, 'Transit', 3) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (11, 'Transit Custom', 3) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (12, 'Transit Connect', 3) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (13, 'Ranger', 3) ON CONFLICT (id) DO NOTHING;

-- Iveco models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (14, 'Daily', 4) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (15, 'Eurocargo', 4) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (16, 'Stralis', 4) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (17, 'S-Way', 4) ON CONFLICT (id) DO NOTHING;

-- MAN models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (18, 'TGE', 5) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (19, 'TGL', 5) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (20, 'TGM', 5) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (21, 'TGS', 5) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (22, 'TGX', 5) ON CONFLICT (id) DO NOTHING;

-- Renault models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (23, 'Master', 6) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (24, 'Trafic', 6) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (25, 'Kangoo', 6) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (26, 'T', 6) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (27, 'D', 6) ON CONFLICT (id) DO NOTHING;

-- Fiat models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (28, 'Ducato', 7) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (29, 'Scudo', 7) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (30, 'Doblo', 7) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (31, 'Fiorino', 7) ON CONFLICT (id) DO NOTHING;

-- Peugeot models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (32, 'Boxer', 8) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (33, 'Expert', 8) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (34, 'Partner', 8) ON CONFLICT (id) DO NOTHING;

-- Citroen models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (35, 'Jumper', 9) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (36, 'Jumpy', 9) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (37, 'Berlingo', 9) ON CONFLICT (id) DO NOTHING;

-- Toyota models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (38, 'Hilux', 10) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (39, 'Proace', 10) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (40, 'Proace City', 10) ON CONFLICT (id) DO NOTHING;

-- Products
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(1, 'Guma 195/55 R15 H', 'Letnja guma za putnička vozila, dimenzije 195/55 R15, indeks brzine H (210 km/h)', 'TIRE-195-55-R15-H', 45.99, 8.5, 'Gume', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(2, 'Guma 225/65 R17 W', 'Letnja guma za SUV vozila, dimenzije 225/65 R17, indeks brzine W (270 km/h)', 'TIRE-225-65-R17-W', 89.99, 12.0, 'Gume', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(3, 'Guma 205/60 R16 Y', 'Letnja guma za putnička vozila, dimenzije 205/60 R16, indeks brzine Y (300 km/h)', 'TIRE-205-60-R16-Y', 65.99, 9.5, 'Gume', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(4, 'Zimska guma 195/65 R15', 'Zimska guma za putnička vozila sa M+S oznakama', 'TIRE-WIN-195-65-R15', 55.99, 9.0, 'Gume', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(5, 'Ulje 5W30 1L', 'Sintetičko motorno ulje 5W30, API SN/CF', 'OIL-5W30-1L', 12.99, 1.0, 'Ulja i maziva', 'L', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(6, 'Ulje 10W40 5L', 'Polusintetičko motorno ulje 10W40, ACEA A3/B4', 'OIL-10W40-5L', 35.99, 5.0, 'Ulja i maziva', 'L', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(7, 'Kočione pločice prednje', 'Kočione pločice za prednje kočnice, univerzalne dimenzije', 'BRAKE-PAD-FRONT', 28.99, 0.8, 'Kočioni sistem', 'set', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(8, 'Kočioni disk 280mm', 'Ventilirani kočioni disk prečnika 280mm', 'BRAKE-DISC-280', 45.99, 6.5, 'Kočioni sistem', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(9, 'Filter ulja', 'Univerzalni filter ulja za putnička vozila', 'FILTER-OIL-UNI', 8.99, 0.3, 'Filteri', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(10, 'Filter vazduha', 'Filter vazduha za putnička vozila', 'FILTER-AIR-UNI', 15.99, 0.2, 'Filteri', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(11, 'Akumulator 60Ah', 'Startni akumulator 12V 60Ah 540A', 'BATTERY-60AH', 85.99, 15.0, 'Električni delovi', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(12, 'Akumulator 74Ah', 'Startni akumulator 12V 74Ah 680A', 'BATTERY-74AH', 99.99, 18.0, 'Električni delovi', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(13, 'Svećice za paljenje', 'Set od 4 svećice za paljenje, iridijum', 'SPARK-PLUG-4SET', 32.99, 0.2, 'Električni delovi', 'set', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(14, 'Antifriz 5L', 'Koncentrat antifriza G12+ crveni', 'COOLANT-G12-5L', 24.99, 5.5, 'Hemija', 'L', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(15, 'Tečnost za stakla 5L', 'Zimska tečnost za pranje stakala -20°C', 'WASHER-WIN-5L', 6.99, 5.0, 'Hemija', 'L', true, NOW()) ON CONFLICT (id) DO NOTHING;

-- Warehouses
INSERT INTO warehouses (id, name, country_id, city_id, street, street_number, latitude, longitude, total_capacity, active, online, last_heartbeat, created_at) VALUES
(1, 'Glavni magacin Beograd', 1, 1, 'Industrijska', '25', 44.8176, 20.4633, 10000.0, true, true, NOW(), NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouses (id, name, country_id, city_id, street, street_number, latitude, longitude, total_capacity, active, online, last_heartbeat, created_at) VALUES
(2, 'Magacin Novi Sad', 1, 2, 'Temerinska', '100', 45.2671, 19.8335, 5000.0, true, true, NOW(), NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouses (id, name, country_id, city_id, street, street_number, latitude, longitude, total_capacity, active, online, last_heartbeat, created_at) VALUES
(3, 'Magacin Zagreb', 2, 6, 'Industrijska zona', '15', 45.8150, 15.9819, 7500.0, true, false, NULL, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouses (id, name, country_id, city_id, street, street_number, latitude, longitude, total_capacity, active, online, last_heartbeat, created_at) VALUES
(4, 'Berlin Distribution Center', 3, 9, 'Industriestrasse', '50', 52.5200, 13.4050, 15000.0, true, true, NOW(), NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouses (id, name, country_id, city_id, street, street_number, latitude, longitude, total_capacity, active, online, last_heartbeat, created_at) VALUES
(5, 'Munich Logistics Hub', 3, 10, 'Logistikweg', '12', 48.1351, 11.5820, 12000.0, true, true, NOW(), NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouses (id, name, country_id, city_id, street, street_number, latitude, longitude, total_capacity, active, online, last_heartbeat, created_at) VALUES
(6, 'Vienna Central Warehouse', 4, 12, 'Lagerstrasse', '8', 48.2082, 16.3738, 8000.0, true, false, NULL, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouses (id, name, country_id, city_id, street, street_number, latitude, longitude, total_capacity, active, online, last_heartbeat, created_at) VALUES
(7, 'Budapest Storage Facility', 5, 14, 'Raktar utca', '22', 47.4979, 19.0402, 6000.0, true, true, NOW(), NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouses (id, name, country_id, city_id, street, street_number, latitude, longitude, total_capacity, active, online, last_heartbeat, created_at) VALUES
(8, 'Nis Regional Warehouse', 1, 3, 'Vojvode Misica', '45', 43.3209, 21.8954, 4000.0, true, true, NOW(), NOW()) ON CONFLICT (id) DO NOTHING;

-- Warehouse Sectors (Belgrade - Warehouse 1)
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(1, 'Sektor A - Gume', 10.0, 25.0, 18.5, NOW(), 3000.0, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(2, 'Sektor B - Ulja i hemija', 5.0, 20.0, 14.2, NOW(), 2000.0, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(3, 'Sektor C - Delovi', 10.0, 30.0, 21.3, NOW(), 5000.0, 1) ON CONFLICT (id) DO NOTHING;

-- Warehouse Sectors (Novi Sad - Warehouse 2)
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(4, 'Sektor A - Gume', 10.0, 25.0, 17.8, NOW(), 2000.0, 2) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(5, 'Sektor B - Ostalo', 5.0, 25.0, 19.1, NOW(), 3000.0, 2) ON CONFLICT (id) DO NOTHING;

-- Warehouse Sectors (Zagreb - Warehouse 3 - Offline)
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(6, 'Glavni sektor', 5.0, 25.0, NULL, NULL, 7500.0, 3) ON CONFLICT (id) DO NOTHING;

-- Warehouse Sectors (Berlin - Warehouse 4)
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(7, 'Zone A - Tires & Wheels', 8.0, 22.0, 16.5, NOW(), 5000.0, 4) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(8, 'Zone B - Lubricants', 5.0, 18.0, 12.3, NOW(), 3000.0, 4) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(9, 'Zone C - Electronics', 15.0, 25.0, 20.0, NOW(), 4000.0, 4) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(10, 'Zone D - General Parts', 10.0, 30.0, 22.1, NOW(), 3000.0, 4) ON CONFLICT (id) DO NOTHING;

-- Warehouse Sectors (Munich - Warehouse 5)
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(11, 'Halle 1 - Reifen', 10.0, 24.0, 17.2, NOW(), 4000.0, 5) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(12, 'Halle 2 - Chemie', 5.0, 18.0, 13.8, NOW(), 3000.0, 5) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(13, 'Halle 3 - Ersatzteile', 10.0, 28.0, 19.5, NOW(), 5000.0, 5) ON CONFLICT (id) DO NOTHING;

-- Warehouse Sectors (Vienna - Warehouse 6 - Offline)
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(14, 'Bereich A', 8.0, 22.0, NULL, NULL, 4000.0, 6) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(15, 'Bereich B', 5.0, 20.0, NULL, NULL, 4000.0, 6) ON CONFLICT (id) DO NOTHING;

-- Warehouse Sectors (Budapest - Warehouse 7)
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(16, 'A Zona - Gumiabroncsok', 10.0, 25.0, 18.9, NOW(), 2500.0, 7) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(17, 'B Zona - Alkatreszek', 10.0, 28.0, 21.4, NOW(), 3500.0, 7) ON CONFLICT (id) DO NOTHING;

-- Warehouse Sectors (Nis - Warehouse 8)
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(18, 'Sektor 1 - Gume', 10.0, 25.0, 19.2, NOW(), 1500.0, 8) ON CONFLICT (id) DO NOTHING;
INSERT INTO warehouse_sectors (id, name, min_temperature, max_temperature, current_temperature, last_temperature_update, capacity, warehouse_id) VALUES
(19, 'Sektor 2 - Delovi', 10.0, 30.0, 22.8, NOW(), 2500.0, 8) ON CONFLICT (id) DO NOTHING;

-- Inventory (stock levels)
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(1, 1, 1, 1, 500, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(2, 2, 1, 1, 300, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(3, 3, 1, 1, 400, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(4, 4, 1, 1, 250, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(5, 5, 1, 2, 1000, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(6, 6, 1, 2, 500, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(7, 7, 1, 3, 800, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(8, 8, 1, 3, 400, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(9, 9, 1, 3, 2000, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(10, 10, 1, 3, 1500, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(11, 11, 1, 3, 200, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(12, 12, 1, 3, 150, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(13, 13, 1, 3, 500, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(14, 14, 1, 2, 600, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(15, 15, 1, 2, 800, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(16, 1, 2, 4, 200, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(17, 2, 2, 4, 150, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(18, 5, 2, 5, 400, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(19, 9, 2, 5, 800, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(20, 1, 3, 6, 300, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(21, 3, 3, 6, 250, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(22, 7, 3, 6, 300, 0, NOW()) ON CONFLICT (id) DO NOTHING;

-- Additional Products
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(16, 'Amortizer prednji', 'Prednji amortizer za putnička vozila', 'SHOCK-FRONT-UNI', 55.99, 3.5, 'Ogibljenje', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(17, 'Amortizer zadnji', 'Zadnji amortizer za putnička vozila', 'SHOCK-REAR-UNI', 48.99, 3.0, 'Ogibljenje', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(18, 'Opruga prednja', 'Prednja opruga za putnička vozila', 'SPRING-FRONT-UNI', 35.99, 2.5, 'Ogibljenje', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(19, 'Kočiona tečnost DOT4 1L', 'Kočiona tečnost DOT4 specifikacija', 'BRAKE-FLUID-DOT4', 9.99, 1.0, 'Hemija', 'L', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(20, 'Ulje za menjač 75W90 1L', 'Sintetičko ulje za menjač 75W90 GL-4/GL-5', 'GEAR-OIL-75W90', 18.99, 1.0, 'Ulja i maziva', 'L', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(21, 'Alternator 14V 90A', 'Alternator za putnička vozila 14V 90A', 'ALT-14V-90A', 125.99, 5.5, 'Električni delovi', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(22, 'Starter 12V 1.4kW', 'Električni starter motor 12V 1.4kW', 'STARTER-12V-1.4KW', 145.99, 4.5, 'Električni delovi', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(23, 'Pumpa za vodu', 'Pumpa rashladne tečnosti za motor', 'WATER-PUMP-UNI', 42.99, 1.2, 'Rashladni sistem', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(24, 'Termostat 87°C', 'Termostat rashladnog sistema 87°C', 'THERMOSTAT-87', 15.99, 0.2, 'Rashladni sistem', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(25, 'Kaiš zupčasti', 'Zupčasti kaiš za razvodni mehanizam', 'TIMING-BELT-UNI', 28.99, 0.3, 'Razvodni mehanizam', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(26, 'Set za kaiš', 'Komplet zupčasti kaiš + zatezač + rolne', 'TIMING-KIT-UNI', 85.99, 1.5, 'Razvodni mehanizam', 'set', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(27, 'Filter goriva diesel', 'Filter goriva za dizel motore', 'FILTER-FUEL-DSL', 22.99, 0.4, 'Filteri', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(28, 'Filter kabine', 'Filter kabine sa aktivnim ugljem', 'FILTER-CABIN-AC', 18.99, 0.2, 'Filteri', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(29, 'Sijalica H7 55W', 'Halogena sijalica H7 55W 12V', 'BULB-H7-55W', 4.99, 0.05, 'Osvetljenje', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO products (id, name, description, sku, price, weight, category, unit, active, created_at) VALUES
(30, 'Sijalica H4 60/55W', 'Halogena sijalica H4 60/55W 12V', 'BULB-H4-60-55W', 5.99, 0.05, 'Osvetljenje', 'kom', true, NOW()) ON CONFLICT (id) DO NOTHING;

-- Additional Inventory for new products
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(23, 16, 1, 3, 150, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(24, 17, 1, 3, 150, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(25, 18, 1, 3, 200, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(26, 19, 1, 2, 400, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(27, 20, 1, 2, 300, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(28, 21, 1, 3, 50, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(29, 22, 1, 3, 40, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(30, 23, 1, 3, 100, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(31, 24, 1, 3, 200, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(32, 25, 1, 3, 300, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(33, 26, 1, 3, 80, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(34, 27, 1, 3, 500, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(35, 28, 1, 3, 600, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(36, 29, 1, 3, 1000, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(37, 30, 1, 3, 800, 0, NOW()) ON CONFLICT (id) DO NOTHING;

-- Inventory in Novi Sad warehouse
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(38, 16, 2, 5, 60, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(39, 17, 2, 5, 60, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(40, 23, 2, 5, 50, 0, NOW()) ON CONFLICT (id) DO NOTHING;

-- Inventory in Zagreb warehouse
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(41, 5, 3, 6, 200, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(42, 6, 3, 6, 100, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(43, 11, 3, 6, 80, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(44, 12, 3, 6, 60, 0, NOW()) ON CONFLICT (id) DO NOTHING;

-- Inventory in Berlin Distribution Center (warehouse 4)
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(45, 1, 4, 7, 800, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(46, 2, 4, 7, 600, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(47, 3, 4, 7, 700, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(48, 5, 4, 8, 1500, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(49, 6, 4, 8, 800, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(50, 11, 4, 9, 300, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(51, 12, 4, 9, 250, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(52, 13, 4, 9, 600, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(53, 7, 4, 10, 500, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(54, 8, 4, 10, 400, 0, NOW()) ON CONFLICT (id) DO NOTHING;

-- Inventory in Munich Logistics Hub (warehouse 5)
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(55, 1, 5, 11, 600, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(56, 4, 5, 11, 400, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(57, 5, 5, 12, 1200, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(58, 14, 5, 12, 500, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(59, 9, 5, 13, 1000, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(60, 10, 5, 13, 800, 0, NOW()) ON CONFLICT (id) DO NOTHING;

-- Inventory in Budapest Storage Facility (warehouse 7)
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(61, 1, 7, 16, 350, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(62, 2, 7, 16, 250, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(63, 7, 7, 17, 300, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(64, 9, 7, 17, 600, 0, NOW()) ON CONFLICT (id) DO NOTHING;

-- Inventory in Nis Regional Warehouse (warehouse 8)
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(65, 1, 8, 18, 200, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(66, 3, 8, 18, 150, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(67, 7, 8, 19, 200, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(68, 9, 8, 19, 400, 0, NOW()) ON CONFLICT (id) DO NOTHING;
INSERT INTO inventory (id, product_id, warehouse_id, sector_id, quantity, reserved_quantity, updated_at) VALUES
(69, 11, 8, 19, 100, 0, NOW()) ON CONFLICT (id) DO NOTHING;


-- Test approved companies for customers
INSERT INTO registration_requests (id, name, country_id, city_id, street, street_number, latitude, longitude, status, rejection_reason, processed_at, owner_id, created_at) VALUES
(1, 'Auto Delovi Miki d.o.o.', 1, 1, 'Bulevar Mihajla Pupina', '10', 44.8200, 20.4500, 'APPROVED', NULL, NOW(), 3, NOW()) ON CONFLICT (id) DO NOTHING;

INSERT INTO registration_requests (id, name, country_id, city_id, street, street_number, latitude, longitude, status, rejection_reason, processed_at, owner_id, created_at) VALUES
(2, 'Miki Transport d.o.o.', 1, 2, 'Futoška', '55', 45.2550, 19.8400, 'APPROVED', NULL, NOW(), 3, NOW()) ON CONFLICT (id) DO NOTHING;

INSERT INTO registration_requests (id, name, country_id, city_id, street, street_number, latitude, longitude, status, rejection_reason, processed_at, owner_id, created_at) VALUES
(3, 'Stefan Auto Servis', 1, 1, 'Kneza Miloša', '100', 44.8050, 20.4650, 'APPROVED', NULL, NOW(), 4, NOW()) ON CONFLICT (id) DO NOTHING;

INSERT INTO registration_requests (id, name, country_id, city_id, street, street_number, latitude, longitude, status, rejection_reason, processed_at, owner_id, created_at) VALUES
(4, 'Test Company Pending', 1, 3, 'Nemanjina', '20', 43.3200, 21.8960, 'PENDING', NULL, NULL, 5, NOW()) ON CONFLICT (id) DO NOTHING;

-- Sample Orders
INSERT INTO orders (id, order_number, customer_id, company_id, total_amount, notes, invoice_path, created_at, processed_at) VALUES
(1, 'ORD-20250115120000-A1B2', 3, 1,  275.94, 'Hitna dostava molim', NULL, '2025-01-15 12:00:00', '2025-01-15 12:00:00') ON CONFLICT (id) DO NOTHING;

INSERT INTO orders (id, order_number, customer_id, company_id, total_amount, notes, invoice_path, created_at, processed_at) VALUES
(2, 'ORD-20250116093000-C3D4', 3, 2,  182.96, NULL, NULL, '2025-01-16 09:30:00', '2025-01-16 09:30:00') ON CONFLICT (id) DO NOTHING;

INSERT INTO orders (id, order_number, customer_id, company_id, total_amount, notes, invoice_path, created_at, processed_at) VALUES
(3, 'ORD-20250117140000-E5F6', 4, 3,  457.93, 'Kontaktirati pre dostave', NULL, '2025-01-17 14:00:00', '2025-01-17 14:00:00') ON CONFLICT (id) DO NOTHING;

-- Order Items for Order 1 (Customer 3 - Miki)
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(1, 1, 1, 4, 45.99, 183.96) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(2, 1, 9, 4, 8.99, 35.96) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(3, 1, 5, 2, 12.99, 25.98) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(4, 1, 15, 1, 6.99, 6.99) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(5, 1, 10, 1, 15.99, 15.99) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(6, 1, 28, 1, 18.99, 18.99) ON CONFLICT (id) DO NOTHING;

-- Order Items for Order 2 (Customer 3 - Miki, second company)
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(7, 2, 7, 2, 28.99, 57.98) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(8, 2, 8, 2, 45.99, 91.98) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(9, 2, 19, 3, 9.99, 29.97) ON CONFLICT (id) DO NOTHING;

-- Order Items for Order 3 (Customer 4 - Stefan)
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(10, 3, 11, 2, 85.99, 171.98) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(11, 3, 22, 1, 145.99, 145.99) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(12, 3, 23, 2, 42.99, 85.98) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(13, 3, 24, 2, 15.99, 31.98) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
(14, 3, 29, 4, 4.99, 19.96) ON CONFLICT (id) DO NOTHING;

-- Reset all sequences to avoid duplicate key violations
-- Note: Only reset sequences for tables that actually use SEQUENCE strategy
-- Tables using TableGenerator (id_generator) are handled separately

-- Update id_generator table for registration_requests
UPDATE id_generator SET next_val = (SELECT COALESCE(MAX(id), 0) + 1 FROM users)
WHERE sequence_name = 'users';

UPDATE id_generator SET next_val = (SELECT COALESCE(MAX(id), 0) + 1 FROM registration_requests)
WHERE sequence_name = 'registration_requests';

UPDATE id_generator SET next_val = (SELECT COALESCE(MAX(id), 0) + 1 FROM orders)
WHERE sequence_name = 'orders';

UPDATE id_generator SET next_val = (SELECT COALESCE(MAX(id), 0) + 1 FROM order_items)
WHERE sequence_name = 'order_items';

UPDATE id_generator SET next_val = (SELECT COALESCE(MAX(id), 0) + 1 FROM order_items)
WHERE sequence_name = 'warehouses';

UPDATE id_generator SET next_val = (SELECT COALESCE(MAX(id), 0) + 1 FROM order_items)
WHERE sequence_name = 'warehouse_sectors';

UPDATE warehouses SET version = 0 WHERE version IS NULL;
UPDATE orders SET version = 0 WHERE version IS NULL;
UPDATE inventory SET version = 0 WHERE version IS NULL;
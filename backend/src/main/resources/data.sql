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
INSERT INTO id_generator (sequence_name, next_val) VALUES ('user', 300) ON CONFLICT (sequence_name) DO NOTHING;

-- 1. ADMIN
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (1, 'Admin', 'Admin', 'admin',
     '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',
     2, 'ADMIN', true, NULL, NULL, 'Manager')
    ON CONFLICT (id) DO NOTHING;


-- 2. CA korisnik 1
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (2, 'Goran', 'Bijelic', 'tetak',
     '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',
     1, 'CA', true, NULL, NULL, 'Manager')
    ON CONFLICT (id) DO NOTHING;


-- 3. CA korisnik 2
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (3, 'Miki', 'Bijelic', 'miki',
     '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',
     0, 'CA', true, NULL, NULL, 'Customer')
    ON CONFLICT (id) DO NOTHING;


-- 4. Običan korisnik
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (4, 'Stefan', 'Nikolić', 'user@company.com',
     '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',
     0, 'COMMON', true, NULL, NULL, 'Customer')
    ON CONFLICT (id) DO NOTHING;


-- 5. Test korisnik
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (5, 'Test', 'Testović', 'test@test.com',
     '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',
     0, 'COMMON', true, NULL, NULL, 'Customer')
    ON CONFLICT (id) DO NOTHING;

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
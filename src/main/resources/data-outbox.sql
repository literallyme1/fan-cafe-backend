-- =========================
-- 👤 USERS
-- =========================
INSERT INTO users (
    id, email, password, nickname, role,
    password_updated_at_epoch_sec,
    password_set, follower_count, following_count,
    introduction,
    created_at, updated_at
)
VALUES
    (1, 'user1@test.com', 'encoded_pw', 'user1', 'USER',
     UNIX_TIMESTAMP(), true, 0, 0,
     '', NOW(), NOW()),

    (2, 'user2@test.com', 'encoded_pw', 'user2', 'USER',
     UNIX_TIMESTAMP(), true, 0, 0,
     '', NOW(), NOW());;



-- =========================
-- 👕 CLOTHES
-- =========================
INSERT INTO merchandises (
    name, description, price, sale_price, stock,
    status, image_url, category,
    created_at, updated_at
)
VALUES
    ('Basic T-Shirt', '편한 기본 티셔츠', 20000, 15000, 50,
     'SALE', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab',
     'CLOTHES', NOW(), NOW()),

    ('Hoodie', '따뜻한 후드', 50000, 42000, 30,
     'SALE', 'https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf',
     'CLOTHES', NOW(), NOW()),

    ('Sweatshirt', '맨투맨', 35000, 30000, 40,
     'SALE', 'https://images.unsplash.com/photo-1585386959984-a4155224a1ad',
     'CLOTHES', NOW(), NOW());;



-- =========================
-- 🧢 HAT
-- =========================
INSERT INTO merchandises (
    name, description, price, sale_price, stock,
    status, image_url, category,
    created_at, updated_at
)
VALUES
    ('Baseball Cap', '볼캡', 18000, 15000, 60,
     'SALE', 'https://images.unsplash.com/photo-1514996937319-344454492b37',
     'HAT', NOW(), NOW()),

    ('Beanie', '비니', 15000, 12000, 70,
     'SALE', 'https://images.unsplash.com/photo-1543076447-215ad9ba6923',
     'HAT', NOW(), NOW()),

    ('Bucket Hat', '버킷햇', 22000, 18000, 45,
     'SALE', 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f',
     'HAT', NOW(), NOW());;
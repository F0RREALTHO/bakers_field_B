-- Coupons + redemption tables and order columns for PostgreSQL

CREATE TABLE IF NOT EXISTS coupons (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(32) NOT NULL UNIQUE,
  type VARCHAR(16) NOT NULL,
  amount NUMERIC(10, 2) NOT NULL,
  min_order_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
  starts_at TIMESTAMPTZ,
  ends_at TIMESTAMPTZ,
  usage_limit INTEGER,
  per_customer_limit INTEGER,
  times_used INTEGER NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS coupon_redemptions (
  id BIGSERIAL PRIMARY KEY,
  coupon_code VARCHAR(32) NOT NULL,
  phone_number VARCHAR(32) NOT NULL,
  redeemed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_code
  ON coupon_redemptions (coupon_code);

CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_code_phone
  ON coupon_redemptions (coupon_code, phone_number);

CREATE TABLE IF NOT EXISTS order_items (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  item_type VARCHAR(16) NOT NULL,
  item_ref_id BIGINT NOT NULL,
  item_name VARCHAR(255) NOT NULL,
  unit_price_inr NUMERIC(10, 2) NOT NULL,
  quantity INTEGER NOT NULL,
  line_total_inr NUMERIC(10, 2) NOT NULL
);

ALTER TABLE order_items
  ADD COLUMN IF NOT EXISTS item_type VARCHAR(16),
  ADD COLUMN IF NOT EXISTS item_ref_id BIGINT,
  ADD COLUMN IF NOT EXISTS item_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS unit_price_inr NUMERIC(10, 2),
  ADD COLUMN IF NOT EXISTS quantity INTEGER,
  ADD COLUMN IF NOT EXISTS line_total_inr NUMERIC(10, 2);

UPDATE order_items
SET
  item_type = COALESCE(item_type, 'PRODUCT'),
  item_ref_id = COALESCE(item_ref_id, 0),
  item_name = COALESCE(item_name, 'Item'),
  unit_price_inr = COALESCE(unit_price_inr, 0),
  quantity = COALESCE(quantity, 1),
  line_total_inr = COALESCE(line_total_inr, COALESCE(unit_price_inr, 0) * COALESCE(quantity, 1));

ALTER TABLE order_items
  ALTER COLUMN item_type SET NOT NULL,
  ALTER COLUMN item_ref_id SET NOT NULL,
  ALTER COLUMN item_name SET NOT NULL,
  ALTER COLUMN unit_price_inr SET NOT NULL,
  ALTER COLUMN quantity SET NOT NULL,
  ALTER COLUMN line_total_inr SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_order_items_order_id
  ON order_items (order_id);

ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS subtotal_amount_inr NUMERIC(10, 2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS discount_amount_inr NUMERIC(10, 2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(32),
  ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20),
  ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(60),
  ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(80),
  ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) DEFAULT 'PENDING',
  ADD COLUMN IF NOT EXISTS sale_discount_amount_inr NUMERIC(10, 2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS sale_name VARCHAR(120);

CREATE TABLE IF NOT EXISTS tags (
  id BIGSERIAL PRIMARY KEY,
  slug VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(80) NOT NULL,
  text_color VARCHAR(20) NOT NULL DEFAULT '#7a3f0c',
  background_color VARCHAR(20) NOT NULL DEFAULT '#fde6c2'
);

CREATE TABLE IF NOT EXISTS product_tag_links (
  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  PRIMARY KEY (product_id, tag_id)
);

CREATE TABLE IF NOT EXISTS combos (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(500),
  image_url VARCHAR(500),
  price_inr NUMERIC(10, 2) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS combo_items (
  id BIGSERIAL PRIMARY KEY,
  combo_id BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  quantity INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sale_configs (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  type VARCHAR(16) NOT NULL,
  amount NUMERIC(10, 2) NOT NULL,
  starts_at TIMESTAMPTZ,
  ends_at TIMESTAMPTZ,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

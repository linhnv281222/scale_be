-- Create scale_manufacturers table
CREATE TABLE IF NOT EXISTS scale_manufacturers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(100),
    website VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(100),
    address VARCHAR(500),
    description VARCHAR(1000),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    INDEX idx_manufacturers_code (code)
);

-- Add manufacturer_id column to scales table
ALTER TABLE scales 
ADD COLUMN manufacturer_id BIGINT,
ADD CONSTRAINT fk_scales_manufacturer 
    FOREIGN KEY (manufacturer_id) 
    REFERENCES scale_manufacturers(id) 
    ON DELETE SET NULL;

-- Create index for manufacturer_id
CREATE INDEX idx_scales_manufacturer ON scales(manufacturer_id);

-- Insert some sample manufacturers
INSERT INTO scale_manufacturers (code, name, country, website, is_active) VALUES
('METTLER', 'Mettler Toledo', 'Switzerland', 'https://www.mt.com', TRUE),
('SARTORIUS', 'Sartorius', 'Germany', 'https://www.sartorius.com', TRUE),
('AND', 'A&D Company', 'Japan', 'https://www.aandd.jp', TRUE),
('OHAUS', 'Ohaus', 'USA', 'https://www.ohaus.com', TRUE),
('RADWAG', 'Radwag', 'Poland', 'https://www.radwag.com', TRUE);

-- Create database if not exists
-- Run this script with postgres superuser: psql -U postgres -f init-db.sql

-- Drop database if exists (careful in production!)
-- DROP DATABASE IF EXISTS scalehub_db;

-- Create database
CREATE DATABASE scalehub_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Connect to the database
\c scalehub_db

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE scalehub_db TO postgres;

COMMENT ON DATABASE scalehub_db IS 'ScaleHub IoT Database - Weighing Scale Management System';

-- ============================================================================
-- OmniSolve Multi-Tenant Database Schema
-- ============================================================================
-- This schema supports multi-organisation SaaS architecture where:
-- - Multiple organisations can use the system independently
-- - Data is isolated by organisation_id
-- - Reference tables (types, statuses, clauses) are shared globally
-- - Business data (documents, employees) is scoped to organisations
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS public;
SET search_path TO public;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

-- ============================================================================
-- GLOBAL REFERENCE TABLES (shared across all organisations)
-- ============================================================================

-- Document Types (Policy, Procedure, Manual, etc.)
CREATE TABLE document_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    requires_clauses BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(255)
);

-- Document Statuses (Draft, Active, Archived, etc.)
CREATE TABLE document_statuses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Departments (Operations, Compliance, Risk, etc.)
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- ISO Clauses (4.4, 5.2, 6.1, etc.)
CREATE TABLE clauses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000)
);

-- Permissions (Global reference data for RBAC)
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255)
);

-- ============================================================================
-- MULTI-TENANT CORE TABLES
-- ============================================================================

-- Organisations (Tenant Root Entity)
-- Each organisation represents a separate company/tenant using the system
CREATE TABLE organisations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Sites (Physical locations within an organisation)
CREATE TABLE sites (
    id BIGSERIAL PRIMARY KEY,
    organisation_id BIGINT NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organisation_id, name)
);

-- Roles (Organisation-scoped roles for RBAC)
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    organisation_id BIGINT NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organisation_id, name)
);

-- Role Permissions (Many-to-Many relationship between roles and permissions)
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Employees (Users within an organisation)
-- Links Cognito users to organisations and provides employee metadata
CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    cognito_sub VARCHAR(255) UNIQUE,
    cognito_username VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50),
    role_id BIGINT REFERENCES roles(id),
    department_id BIGINT REFERENCES departments(id),
    organisation_id BIGINT NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    site_id BIGINT REFERENCES sites(id),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organisation_id, email)
);

-- ============================================================================
-- BUSINESS DATA TABLES (scoped to organisations)
-- ============================================================================

-- Documents (Organisation-scoped controlled documents)
-- Each document belongs to exactly one organisation
-- Document numbers are unique per organisation (not globally)
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id BIGINT NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    document_number VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(1000),
    type_id BIGINT NOT NULL REFERENCES document_types(id),
    department_id BIGINT NOT NULL REFERENCES departments(id),
    status_id BIGINT NOT NULL REFERENCES document_statuses(id),
    owner_id VARCHAR(255) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    next_review_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organisation_id, document_number)
);

-- Document Versions (File versions stored in S3)
CREATE TABLE document_versions (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, version_number)
);

-- Document-Clause Links (Many-to-Many relationship)
CREATE TABLE document_clause_links (
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    clause_id BIGINT NOT NULL REFERENCES clauses(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, clause_id)
);

-- Document Reviews (Review tracking for documents)
CREATE TABLE document_reviews (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    reviewer_id VARCHAR(255) NOT NULL,
    due_date DATE NOT NULL,
    completed_at TIMESTAMPTZ,
    review_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Audit Logs (Organisation-scoped audit trail)
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    organisation_id BIGINT REFERENCES organisations(id) ON DELETE CASCADE,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    details JSONB,
    performed_by VARCHAR(255) NOT NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- Employee indexes
CREATE INDEX idx_employees_organisation_id ON employees(organisation_id);
CREATE INDEX idx_employees_cognito_sub ON employees(cognito_sub);
CREATE INDEX idx_employees_status ON employees(status);
CREATE INDEX idx_employees_email ON employees(email);

-- Site indexes
CREATE INDEX idx_sites_organisation_id ON sites(organisation_id);

-- Document indexes
CREATE INDEX idx_documents_organisation_id ON documents(organisation_id);
CREATE INDEX idx_documents_department_id ON documents(department_id);
CREATE INDEX idx_documents_status_id ON documents(status_id);
CREATE INDEX idx_documents_owner_id ON documents(owner_id);
CREATE INDEX idx_documents_created_by ON documents(created_by);

-- Composite indexes for common multi-tenant queries
CREATE INDEX idx_documents_org_status ON documents(organisation_id, status_id);
CREATE INDEX idx_documents_org_department ON documents(organisation_id, department_id);
CREATE INDEX idx_documents_org_type ON documents(organisation_id, type_id);

-- Audit log indexes
CREATE INDEX idx_audit_logs_organisation_id ON audit_logs(organisation_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_logs_performed_at ON audit_logs(performed_at);

-- Document review indexes
CREATE INDEX idx_document_reviews_document_id ON document_reviews(document_id);
CREATE INDEX idx_document_reviews_reviewer_id ON document_reviews(reviewer_id);
CREATE INDEX idx_document_reviews_due_date ON document_reviews(due_date);

-- ============================================================================
-- SEED DATA - GLOBAL REFERENCE TABLES
-- ============================================================================

-- Document Statuses
INSERT INTO document_statuses (name) VALUES
    ('Draft'),
    ('Pending Approval'),
    ('Active'),
    ('Superseded'),
    ('Archived');

-- Document Types
INSERT INTO document_types (name, description, requires_clauses) VALUES
    ('Policy', 'High-level management policies', true),
    ('Procedure', 'Step-by-step operational procedures', true),
    ('Manual', 'Management system manuals', true),
    ('Work Instruction', 'Detailed task instructions', true),
    ('Form', 'Operational forms', false),
    ('Record', 'Evidence records', false),
    ('Checklist', 'Operational checklists', false);

-- Departments
INSERT INTO departments (name, description) VALUES
    ('Operations', 'Operational controls and procedures'),
    ('Compliance', 'Compliance and regulatory management'),
    ('Risk', 'Enterprise risk oversight'),
    ('HR', 'People and talent management'),
    ('Finance', 'Financial controls and reporting');

-- ISO Clauses
INSERT INTO clauses (code, title, description) VALUES
    ('4.4', 'Quality management system and its processes', 'Define and control QMS processes and interactions.'),
    ('5.2', 'Quality policy', 'Establish, communicate, and maintain the quality policy.'),
    ('6.1', 'Actions to address risks and opportunities', 'Plan actions to manage risk and opportunities.'),
    ('7.5', 'Documented information', 'Create, update, and control documented information.'),
    ('9.2', 'Internal audit', 'Conduct internal audits to verify QMS effectiveness.');

-- ============================================================================
-- SEED DATA - DEMO ORGANISATION
-- ============================================================================

-- Create demo organisation for development/testing
INSERT INTO organisations (name, created_at, updated_at)
VALUES ('OmniSolve Demo Organisation', NOW(), NOW());

-- Create demo site
INSERT INTO sites (organisation_id, name, created_at, updated_at)
VALUES (
    (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation'),
    'Head Office',
    NOW(),
    NOW()
);

-- Role indexes
CREATE INDEX idx_roles_organisation_id ON roles(organisation_id);
CREATE INDEX idx_employees_role_id ON employees(role_id);

-- Role permission indexes
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Permissions (Global reference data for RBAC)
INSERT INTO permissions (code, name, description) VALUES
    ('view_dashboard', 'View Dashboard', 'Access dashboard overview'),
    ('manage_documents', 'Manage Documents', 'Create and manage documents'),
    ('manage_employees', 'Manage Employees', 'Create and manage employees'),
    ('manage_risks', 'Manage Risks', 'Manage enterprise risks'),
    ('manage_audits', 'Manage Audits', 'Create and manage audits'),
    ('view_reports', 'View Reports', 'Access reporting dashboards'),
    ('manage_settings', 'Manage Settings', 'Configure system settings');

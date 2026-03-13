-- ============================================================================
-- OmniSolve Multi-Tenant Database Schema
-- ============================================================================
-- This schema supports multi-organisation SaaS architecture where:
-- - Multiple organisations can use the system independently
-- - Data is isolated by organisation_id
-- - Reference tables (types, statuses, standards, clauses) are shared globally
-- - Business data (documents, employees, incidents) is scoped to organisations
-- - Multi-standard compliance (ISO 9001, ISO 14001, ISO 45001)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS public;
SET search_path TO public;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

-- ============================================================================
-- GLOBAL REFERENCE TABLES (shared across all organisations)
-- ============================================================================

-- Compliance Standards (ISO 9001, ISO 14001, ISO 45001, etc.)
CREATE TABLE standards (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    version VARCHAR(50),
    published_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ISO Clauses (linked to standards for multi-standard support)
CREATE TABLE clauses (
    id BIGSERIAL PRIMARY KEY,
    standard_id BIGINT NOT NULL REFERENCES standards(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    parent_code VARCHAR(50),
    level INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (standard_id, code)
);

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

-- Permissions (Global reference data for RBAC)
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255)
);

-- Incident Types (Injury, Environmental, Quality, Security, Near Miss)
CREATE TABLE incident_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Incident Severity Levels
CREATE TABLE incident_severities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    level INTEGER NOT NULL
);

-- Incident Statuses
CREATE TABLE incident_statuses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
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
-- DOCUMENT CONTROL TABLES (scoped to organisations)
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

-- ============================================================================
-- INCIDENT MANAGEMENT TABLES (scoped to organisations)
-- ============================================================================

-- Incidents (Organisation-scoped incident reports)
CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id BIGINT NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    incident_number VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type_id BIGINT NOT NULL REFERENCES incident_types(id),
    severity_id BIGINT NOT NULL REFERENCES incident_severities(id),
    status_id BIGINT NOT NULL REFERENCES incident_statuses(id),
    department_id BIGINT REFERENCES departments(id),
    site_id BIGINT REFERENCES sites(id),
    reported_by VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ,
    reported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_investigator VARCHAR(255),
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organisation_id, incident_number)
);

-- Incident Attachments (Files stored in S3)
CREATE TABLE incident_attachments (
    id BIGSERIAL PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    s3_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Incident Investigations (Root cause analysis)
CREATE TABLE incident_investigations (
    id BIGSERIAL PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    investigator_id VARCHAR(255) NOT NULL,
    analysis_method VARCHAR(100),
    root_cause TEXT,
    findings TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Incident Corrective Actions
CREATE TABLE incident_actions (
    id BIGSERIAL PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_to VARCHAR(255),
    due_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'open',
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Incident Comments / Timeline
CREATE TABLE incident_comments (
    id BIGSERIAL PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    comment TEXT NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- AUDIT TRAIL
-- ============================================================================

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

-- Standards and Clauses indexes
CREATE INDEX idx_clauses_standard_id ON clauses(standard_id);
CREATE INDEX idx_clauses_parent_code ON clauses(parent_code);
CREATE INDEX idx_clauses_code ON clauses(code);

-- Employee indexes
CREATE INDEX idx_employees_organisation_id ON employees(organisation_id);
CREATE INDEX idx_employees_cognito_sub ON employees(cognito_sub);
CREATE INDEX idx_employees_status ON employees(status);
CREATE INDEX idx_employees_email ON employees(email);

-- Site indexes
CREATE INDEX idx_sites_organisation_id ON sites(organisation_id);

-- Role indexes
CREATE INDEX idx_roles_organisation_id ON roles(organisation_id);
CREATE INDEX idx_employees_role_id ON employees(role_id);

-- Role permission indexes
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

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

-- Document review indexes
CREATE INDEX idx_document_reviews_document_id ON document_reviews(document_id);
CREATE INDEX idx_document_reviews_reviewer_id ON document_reviews(reviewer_id);
CREATE INDEX idx_document_reviews_due_date ON document_reviews(due_date);

-- Document clause link indexes
CREATE INDEX idx_document_clause_links_document_id ON document_clause_links(document_id);
CREATE INDEX idx_document_clause_links_clause_id ON document_clause_links(clause_id);

-- Incident indexes
CREATE INDEX idx_incidents_organisation_id ON incidents(organisation_id);
CREATE INDEX idx_incidents_status_id ON incidents(status_id);
CREATE INDEX idx_incidents_severity_id ON incidents(severity_id);
CREATE INDEX idx_incidents_department_id ON incidents(department_id);
CREATE INDEX idx_incidents_site_id ON incidents(site_id);
CREATE INDEX idx_incidents_reported_by ON incidents(reported_by);

-- Incident relationship indexes
CREATE INDEX idx_incident_attachments_incident_id ON incident_attachments(incident_id);
CREATE INDEX idx_incident_investigations_incident_id ON incident_investigations(incident_id);
CREATE INDEX idx_incident_actions_incident_id ON incident_actions(incident_id);
CREATE INDEX idx_incident_comments_incident_id ON incident_comments(incident_id);

-- Audit log indexes
CREATE INDEX idx_audit_logs_organisation_id ON audit_logs(organisation_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_logs_performed_at ON audit_logs(performed_at);


-- ============================================================================
-- OmniSolve Assurance - Inspections Module
-- ============================================================================

-- ============================================================================
-- INSPECTION TYPES
-- Allows inspections engine to support audits, safety walks etc.
-- ============================================================================

CREATE TABLE inspection_types (
                                  id BIGSERIAL PRIMARY KEY,
                                  code VARCHAR(100) NOT NULL UNIQUE,
                                  name VARCHAR(255) NOT NULL,
                                  description VARCHAR(1000)
);


-- ============================================================================
-- INSPECTION SEVERITIES
-- ============================================================================

CREATE TABLE inspection_severities (
                                       id BIGSERIAL PRIMARY KEY,
                                       name VARCHAR(50) NOT NULL UNIQUE,
                                       level INTEGER NOT NULL
);


-- ============================================================================
-- ASSET TYPES (global reference data)
-- ============================================================================

CREATE TABLE asset_types (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(255) NOT NULL UNIQUE,
                             description VARCHAR(1000)
);


-- ============================================================================
-- ASSETS (organisation-scoped inspectable items)
-- ============================================================================

CREATE TABLE assets (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        organisation_id BIGINT NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
                        asset_type_id BIGINT NOT NULL REFERENCES asset_types(id),
                        name VARCHAR(255) NOT NULL,
                        asset_tag VARCHAR(100),
                        serial_number VARCHAR(100),
                        site_id BIGINT REFERENCES sites(id),
                        department_id BIGINT REFERENCES departments(id),
                        status VARCHAR(50) NOT NULL DEFAULT 'Active',
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                        UNIQUE (organisation_id, asset_tag)
);


-- ============================================================================
-- INSPECTIONS
-- ============================================================================

CREATE TABLE inspections (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             organisation_id BIGINT NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
                             asset_id UUID NOT NULL REFERENCES assets(id),
                             inspection_type_id BIGINT REFERENCES inspection_types(id),

                             inspection_number VARCHAR(100) NOT NULL,
                             title VARCHAR(255) NOT NULL,
                             inspector_id VARCHAR(255),

                             status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',

                             scheduled_at TIMESTAMPTZ,
                             started_at TIMESTAMPTZ,
                             completed_at TIMESTAMPTZ,

                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                             UNIQUE (organisation_id, inspection_number)
);


-- ============================================================================
-- INSPECTION CHECKLIST TEMPLATES
-- ============================================================================

CREATE TABLE inspection_checklists (
                                       id BIGSERIAL PRIMARY KEY,
                                       name VARCHAR(255) NOT NULL,
                                       description VARCHAR(1000),
                                       asset_type_id BIGINT REFERENCES asset_types(id)
);


-- ============================================================================
-- CHECKLIST ITEMS
-- ============================================================================

CREATE TABLE inspection_checklist_items (
                                            id BIGSERIAL PRIMARY KEY,
                                            checklist_id BIGINT NOT NULL REFERENCES inspection_checklists(id) ON DELETE CASCADE,
                                            title VARCHAR(255) NOT NULL,
                                            description TEXT,
                                            sort_order INT NOT NULL DEFAULT 0
);


-- ============================================================================
-- INSPECTION RESULTS
-- ============================================================================

CREATE TABLE inspection_items (
                                  id BIGSERIAL PRIMARY KEY,
                                  inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE CASCADE,
                                  checklist_item_id BIGINT REFERENCES inspection_checklist_items(id),

                                  status VARCHAR(50) NOT NULL,
                                  notes TEXT
);


-- ============================================================================
-- INSPECTION FINDINGS
-- ============================================================================

CREATE TABLE inspection_findings (
                                     id BIGSERIAL PRIMARY KEY,
                                     inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE CASCADE,
                                     clause_id BIGINT REFERENCES clauses(id),
                                     severity_id BIGINT REFERENCES inspection_severities(id),

                                     description TEXT NOT NULL,
                                     action_required BOOLEAN NOT NULL DEFAULT FALSE,

                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================================
-- INSPECTION ATTACHMENTS
-- ============================================================================

CREATE TABLE inspection_attachments (
                                        id BIGSERIAL PRIMARY KEY,
                                        inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE CASCADE,

                                        s3_key VARCHAR(500) NOT NULL,
                                        file_name VARCHAR(255) NOT NULL,
                                        file_size BIGINT,
                                        mime_type VARCHAR(100),

                                        uploaded_by VARCHAR(255) NOT NULL,
                                        uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================================
-- OPTIONAL TAGGING SYSTEM (future flexibility)
-- ============================================================================

CREATE TABLE tags (
                      id BIGSERIAL PRIMARY KEY,
                      name VARCHAR(100) NOT NULL UNIQUE,
                      category VARCHAR(100)
);

CREATE TABLE inspection_tags (
                                 inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE CASCADE,
                                 tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                                 PRIMARY KEY (inspection_id, tag_id)
);


-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_assets_org ON assets(organisation_id);
CREATE INDEX idx_inspections_org ON inspections(organisation_id);
CREATE INDEX idx_inspections_asset ON inspections(asset_id);

CREATE INDEX idx_inspection_items_inspection
    ON inspection_items(inspection_id);

CREATE INDEX idx_inspection_findings_inspection
    ON inspection_findings(inspection_id);

CREATE INDEX idx_inspection_attachments_inspection
    ON inspection_attachments(inspection_id);
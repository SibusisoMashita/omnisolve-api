-- ============================================================================
-- V5__incident_management.sql
-- OmniSolve Incident Management Module
-- ============================================================================

SET search_path TO public;

-- ============================================================================
-- GLOBAL REFERENCE TABLES
-- ============================================================================

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
-- INCIDENTS (Organisation Scoped)
-- ============================================================================

CREATE TABLE incidents (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                           organisation_id BIGINT NOT NULL
                               REFERENCES organisations(id) ON DELETE CASCADE,

                           incident_number VARCHAR(100) NOT NULL,

                           title VARCHAR(255) NOT NULL,
                           description TEXT,

                           type_id BIGINT NOT NULL
                               REFERENCES incident_types(id),

                           severity_id BIGINT NOT NULL
                               REFERENCES incident_severities(id),

                           status_id BIGINT NOT NULL
                               REFERENCES incident_statuses(id),

                           department_id BIGINT
                               REFERENCES departments(id),

                           site_id BIGINT
                               REFERENCES sites(id),

                           reported_by VARCHAR(255) NOT NULL,

                           occurred_at TIMESTAMPTZ,

                           reported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                           assigned_investigator VARCHAR(255),

                           closed_at TIMESTAMPTZ,

                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                           UNIQUE (organisation_id, incident_number)
);

-- ============================================================================
-- INCIDENT ATTACHMENTS
-- ============================================================================

CREATE TABLE incident_attachments (
                                      id BIGSERIAL PRIMARY KEY,

                                      incident_id UUID NOT NULL
                                          REFERENCES incidents(id) ON DELETE CASCADE,

                                      s3_key VARCHAR(500) NOT NULL,

                                      file_name VARCHAR(255) NOT NULL,

                                      file_size BIGINT,

                                      mime_type VARCHAR(100),

                                      uploaded_by VARCHAR(255) NOT NULL,

                                      uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INCIDENT INVESTIGATIONS
-- ============================================================================

CREATE TABLE incident_investigations (
                                         id BIGSERIAL PRIMARY KEY,

                                         incident_id UUID NOT NULL
                                             REFERENCES incidents(id) ON DELETE CASCADE,

                                         investigator_id VARCHAR(255) NOT NULL,

                                         analysis_method VARCHAR(100),

                                         root_cause TEXT,

                                         findings TEXT,

                                         started_at TIMESTAMPTZ,

                                         completed_at TIMESTAMPTZ,

                                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INCIDENT CORRECTIVE ACTIONS
-- ============================================================================

CREATE TABLE incident_actions (
                                  id BIGSERIAL PRIMARY KEY,

                                  incident_id UUID NOT NULL
                                      REFERENCES incidents(id) ON DELETE CASCADE,

                                  title VARCHAR(255) NOT NULL,

                                  description TEXT,

                                  assigned_to VARCHAR(255),

                                  due_date DATE,

                                  status VARCHAR(50) NOT NULL DEFAULT 'open',

                                  completed_at TIMESTAMPTZ,

                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INCIDENT COMMENTS / TIMELINE
-- ============================================================================

CREATE TABLE incident_comments (
                                   id BIGSERIAL PRIMARY KEY,

                                   incident_id UUID NOT NULL
                                       REFERENCES incidents(id) ON DELETE CASCADE,

                                   comment TEXT NOT NULL,

                                   created_by VARCHAR(255) NOT NULL,

                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_incidents_organisation_id
    ON incidents(organisation_id);

CREATE INDEX idx_incidents_status_id
    ON incidents(status_id);

CREATE INDEX idx_incidents_severity_id
    ON incidents(severity_id);

CREATE INDEX idx_incidents_department_id
    ON incidents(department_id);

CREATE INDEX idx_incidents_site_id
    ON incidents(site_id);

CREATE INDEX idx_incidents_reported_by
    ON incidents(reported_by);

CREATE INDEX idx_incident_attachments_incident_id
    ON incident_attachments(incident_id);

CREATE INDEX idx_incident_investigations_incident_id
    ON incident_investigations(incident_id);

CREATE INDEX idx_incident_actions_incident_id
    ON incident_actions(incident_id);

CREATE INDEX idx_incident_comments_incident_id
    ON incident_comments(incident_id);

-- ============================================================================
-- DEFAULT REFERENCE DATA
-- ============================================================================

INSERT INTO incident_types (name, description) VALUES
                                                   ('Injury', 'Workplace injury or health incident'),
                                                   ('Environmental', 'Environmental spill or impact'),
                                                   ('Quality', 'Product or service quality issue'),
                                                   ('Security', 'Security breach or threat'),
                                                   ('Near Miss', 'Incident that could have caused harm but did not');

INSERT INTO incident_severities (name, level) VALUES
                                                  ('Low', 1),
                                                  ('Medium', 2),
                                                  ('High', 3),
                                                  ('Critical', 4);

INSERT INTO incident_statuses (name) VALUES
                                         ('Reported'),
                                         ('Under Review'),
                                         ('Investigation'),
                                         ('Action Required'),
                                         ('Closed');
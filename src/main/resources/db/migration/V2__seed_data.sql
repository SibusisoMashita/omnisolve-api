-- ============================================================================
-- DEMO ORGANISATION RBAC SEED DATA
-- ============================================================================
-- Seeds roles and role permissions for the OmniSolve Demo Organisation
-- ============================================================================
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


-- Permissions (Global reference data for RBAC)
INSERT INTO permissions (code, name, description) VALUES
                                                      ('view_dashboard', 'View Dashboard', 'Access dashboard overview'),
                                                      ('manage_documents', 'Manage Documents', 'Create and manage documents'),
                                                      ('manage_employees', 'Manage Employees', 'Create and manage employees'),
                                                      ('manage_risks', 'Manage Risks', 'Manage enterprise risks'),
                                                      ('manage_audits', 'Manage Audits', 'Create and manage audits'),
                                                      ('view_reports', 'View Reports', 'Access reporting dashboards'),
                                                      ('manage_settings', 'Manage Settings', 'Configure system settings');


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
-- Insert demo roles for OmniSolve Demo Organisation
INSERT INTO roles (organisation_id, name, description, created_at, updated_at)
SELECT 
    org.id,
    role_data.name,
    role_data.description,
    NOW(),
    NOW()
FROM (
    VALUES
        ('Administrator', 'Full system access with all permissions'),
        ('Safety Manager', 'Manage risks, audits, and view reports'),
        ('Supervisor', 'View dashboard, manage risks, and view reports'),
        ('Employee', 'Basic access to dashboard and reports')
) AS role_data(name, description)
CROSS JOIN (
    SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation'
) AS org
WHERE NOT EXISTS (
    SELECT 1 FROM roles r
    WHERE r.organisation_id = org.id
      AND r.name = role_data.name
);

-- Assign permissions to Administrator role (ALL permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    r.id AS role_id,
    p.id AS permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
  AND r.name = 'Administrator'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Assign permissions to Safety Manager role
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    r.id AS role_id,
    p.id AS permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
  AND r.name = 'Safety Manager'
  AND p.code IN ('view_dashboard', 'manage_risks', 'manage_audits', 'view_reports')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Assign permissions to Supervisor role
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    r.id AS role_id,
    p.id AS permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
  AND r.name = 'Supervisor'
  AND p.code IN ('view_dashboard', 'manage_risks', 'view_reports')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Assign permissions to Employee role
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    r.id AS role_id,
    p.id AS permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
  AND r.name = 'Employee'
  AND p.code IN ('view_dashboard', 'view_reports')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

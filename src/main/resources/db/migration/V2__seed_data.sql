-- ============================================================================
-- DEMO ORGANISATION RBAC SEED DATA
-- ============================================================================
-- Seeds roles and role permissions for the OmniSolve Demo Organisation
-- ============================================================================

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

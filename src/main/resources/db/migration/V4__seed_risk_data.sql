-- ============================================================================
-- Risk Management Seed Data
-- ============================================================================
-- Reference data for risk categories, severities, likelihoods, and RBAC
-- permissions for the Risk pillar.
-- ============================================================================


-- ============================================================================
-- RISK CATEGORIES
-- ============================================================================

INSERT INTO risk_categories (name, description) VALUES
    ('Strategic',       'Risks related to high-level organisational goals and direction'),
    ('Operational',     'Risks arising from day-to-day processes and activities'),
    ('Financial',       'Risks related to financial loss, fraud, or mismanagement'),
    ('Compliance',      'Risks of failing to meet legal, regulatory, or contractual obligations'),
    ('Reputational',    'Risks that could damage the organisation''s public image or brand'),
    ('Environmental',   'Risks of environmental harm or non-compliance with environmental standards'),
    ('Health & Safety', 'Risks to the health and safety of employees, contractors, or the public'),
    ('Technology',      'Risks related to IT systems, data security, or cyber threats')
ON CONFLICT (name) DO NOTHING;


-- ============================================================================
-- RISK SEVERITIES
-- Level scale: 1 (Negligible) → 5 (Catastrophic)
-- ============================================================================

INSERT INTO risk_severities (name, level) VALUES
    ('Negligible',    1),
    ('Minor',         2),
    ('Moderate',      3),
    ('Major',         4),
    ('Catastrophic',  5)
ON CONFLICT (name) DO NOTHING;


-- ============================================================================
-- RISK LIKELIHOODS
-- Level scale: 1 (Rare) → 5 (Almost Certain)
-- ============================================================================

INSERT INTO risk_likelihoods (name, level) VALUES
    ('Rare',           1),
    ('Unlikely',       2),
    ('Possible',       3),
    ('Likely',         4),
    ('Almost Certain', 5)
ON CONFLICT (name) DO NOTHING;


-- ============================================================================
-- RISK PERMISSIONS
-- Fine-grained RBAC codes for the Risk pillar.
-- ============================================================================

INSERT INTO permissions (code, name, description) VALUES
    ('risk.view',   'View Risks',   'View risks, controls, and attachments'),
    ('risk.create', 'Create Risks', 'Create new risk records'),
    ('risk.update', 'Update Risks', 'Edit existing risk records and controls'),
    ('risk.delete', 'Delete Risks', 'Delete risk records')
ON CONFLICT (code) DO NOTHING;


-- ============================================================================
-- GRANT RISK PERMISSIONS TO RELEVANT ROLES
-- Administrator gets all four; Safety/Environmental Managers get view + create + update.
-- ============================================================================

INSERT INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id
    FROM roles r
    CROSS JOIN permissions p
    WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
    AND r.name = 'Administrator'
    AND p.code IN ('risk.view', 'risk.create', 'risk.update', 'risk.delete')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id
    FROM roles r
    CROSS JOIN permissions p
    WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
    AND r.name IN ('Safety Manager', 'Environmental Manager')
    AND p.code IN ('risk.view', 'risk.create', 'risk.update')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id
    FROM roles r
    CROSS JOIN permissions p
    WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
    AND r.name IN ('Supervisor', 'Quality Manager')
    AND p.code = 'risk.view'
ON CONFLICT DO NOTHING;

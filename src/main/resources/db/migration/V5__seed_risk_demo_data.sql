-- ============================================================================
-- Risk Demo Data
-- ============================================================================
-- Demo risks, controls linked to the OmniSolve Demo Organisation.
-- Reference rows (categories, severities, likelihoods) were seeded in V4.
-- ============================================================================


-- ============================================================================
-- DEMO RISKS
-- ============================================================================

INSERT INTO risks (
    organisation_id,
    title,
    description,
    category_id,
    likelihood_id,
    severity_id,
    risk_score,
    owner_id,
    status,
    review_date,
    identified_at,
    created_at,
    updated_at
)
SELECT
    (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation'),
    'Machine entanglement hazard',
    'Workers may get caught in rotating machine parts during maintenance operations.',
    (SELECT id FROM risk_categories WHERE name = 'Health & Safety'),
    (SELECT id FROM risk_likelihoods WHERE name = 'Likely'),
    (SELECT id FROM risk_severities  WHERE name = 'Major'),
    (SELECT l.level * s.level
     FROM risk_likelihoods l, risk_severities s
     WHERE l.name = 'Likely' AND s.name = 'Major'),
    'operations.manager',
    'OPEN',
    CURRENT_DATE + INTERVAL '30 days',
    NOW(),
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM risks WHERE title = 'Machine entanglement hazard'
);

INSERT INTO risks (
    organisation_id,
    title,
    description,
    category_id,
    likelihood_id,
    severity_id,
    risk_score,
    owner_id,
    status,
    review_date,
    identified_at,
    created_at,
    updated_at
)
SELECT
    (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation'),
    'Chemical spill risk',
    'Storage containers may leak causing environmental contamination of surrounding area.',
    (SELECT id FROM risk_categories WHERE name = 'Environmental'),
    (SELECT id FROM risk_likelihoods WHERE name = 'Possible'),
    (SELECT id FROM risk_severities  WHERE name = 'Moderate'),
    (SELECT l.level * s.level
     FROM risk_likelihoods l, risk_severities s
     WHERE l.name = 'Possible' AND s.name = 'Moderate'),
    'safety.officer',
    'OPEN',
    CURRENT_DATE + INTERVAL '60 days',
    NOW(),
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM risks WHERE title = 'Chemical spill risk'
);

INSERT INTO risks (
    organisation_id,
    title,
    description,
    category_id,
    likelihood_id,
    severity_id,
    risk_score,
    owner_id,
    status,
    review_date,
    identified_at,
    created_at,
    updated_at
)
SELECT
    (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation'),
    'Regulatory reporting delay',
    'Compliance reports may not be submitted before regulatory deadlines due to manual processes.',
    (SELECT id FROM risk_categories WHERE name = 'Compliance'),
    (SELECT id FROM risk_likelihoods WHERE name = 'Possible'),
    (SELECT id FROM risk_severities  WHERE name = 'Minor'),
    (SELECT l.level * s.level
     FROM risk_likelihoods l, risk_severities s
     WHERE l.name = 'Possible' AND s.name = 'Minor'),
    'compliance.manager',
    'OPEN',
    CURRENT_DATE + INTERVAL '45 days',
    NOW(),
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM risks WHERE title = 'Regulatory reporting delay'
);

INSERT INTO risks (
    organisation_id,
    title,
    description,
    category_id,
    likelihood_id,
    severity_id,
    risk_score,
    owner_id,
    status,
    review_date,
    identified_at,
    created_at,
    updated_at
)
SELECT
    (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation'),
    'Data breach via third-party vendor',
    'Vendor systems with access to internal data may be compromised, exposing sensitive records.',
    (SELECT id FROM risk_categories WHERE name = 'Technology'),
    (SELECT id FROM risk_likelihoods WHERE name = 'Unlikely'),
    (SELECT id FROM risk_severities  WHERE name = 'Catastrophic'),
    (SELECT l.level * s.level
     FROM risk_likelihoods l, risk_severities s
     WHERE l.name = 'Unlikely' AND s.name = 'Catastrophic'),
    'it.manager',
    'OPEN',
    CURRENT_DATE + INTERVAL '14 days',
    NOW(),
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM risks WHERE title = 'Data breach via third-party vendor'
);


-- ============================================================================
-- DEMO RISK CONTROLS
-- ============================================================================

INSERT INTO risk_controls (risk_id, description, control_owner, created_at)
SELECT
    r.id,
    'Install machine safety guards on all conveyor belts and rotating parts',
    'maintenance.manager',
    NOW()
FROM risks r
WHERE r.title = 'Machine entanglement hazard'
AND NOT EXISTS (
    SELECT 1 FROM risk_controls rc
    WHERE rc.risk_id = r.id
    AND rc.description = 'Install machine safety guards on all conveyor belts and rotating parts'
);

INSERT INTO risk_controls (risk_id, description, control_owner, created_at)
SELECT
    r.id,
    'Conduct mandatory lockout/tagout training for all maintenance staff',
    'safety.officer',
    NOW()
FROM risks r
WHERE r.title = 'Machine entanglement hazard'
AND NOT EXISTS (
    SELECT 1 FROM risk_controls rc
    WHERE rc.risk_id = r.id
    AND rc.description = 'Conduct mandatory lockout/tagout training for all maintenance staff'
);

INSERT INTO risk_controls (risk_id, description, control_owner, created_at)
SELECT
    r.id,
    'Implement weekly chemical storage inspection checklist',
    'environment.officer',
    NOW()
FROM risks r
WHERE r.title = 'Chemical spill risk'
AND NOT EXISTS (
    SELECT 1 FROM risk_controls rc
    WHERE rc.risk_id = r.id
    AND rc.description = 'Implement weekly chemical storage inspection checklist'
);

INSERT INTO risk_controls (risk_id, description, control_owner, created_at)
SELECT
    r.id,
    'Implement vendor security assessment process before granting data access',
    'it.manager',
    NOW()
FROM risks r
WHERE r.title = 'Data breach via third-party vendor'
AND NOT EXISTS (
    SELECT 1 FROM risk_controls rc
    WHERE rc.risk_id = r.id
    AND rc.description = 'Implement vendor security assessment process before granting data access'
);

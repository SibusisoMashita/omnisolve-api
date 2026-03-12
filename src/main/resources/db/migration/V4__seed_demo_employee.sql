-- ============================================================================
-- SEED DATA - DEMO EMPLOYEES
-- ============================================================================
-- Adds demo employees linked to Cognito users
-- Safe to run multiple times (idempotent)
-- ============================================================================

INSERT INTO employees (
    cognito_sub,
    email,
    first_name,
    department_id,
    organisation_id,
    site_id,
    status,
    created_at,
    updated_at
)
SELECT
    v.cognito_sub,
    v.email,
    v.first_name,
    (SELECT id FROM departments WHERE name = 'Operations'),
    org.id,
    site.id,
    'active',
    NOW(),
    NOW()
FROM (
         VALUES
             ('84d8f448-a0b1-701a-4694-0fc8ac94a614','sibusiso.mashita@gmail.com','Sibusiso'),
             ('44a844f8-1081-70c1-895f-fd8900537782','lefa@omnisolve.co.za','Lefa'),
             ('a4f81438-80e1-70fb-b051-d066017e27e4','siphiwe@omnisolve.co.za','Siphiwe')
     ) AS v(cognito_sub,email,first_name)

         CROSS JOIN (
    SELECT id
    FROM organisations
    WHERE name = 'OmniSolve Demo Organisation'
) org

         CROSS JOIN (
    SELECT id
    FROM sites
    WHERE name = 'Head Office'
      AND organisation_id = (
        SELECT id
        FROM organisations
        WHERE name = 'OmniSolve Demo Organisation'
    )
) site

WHERE NOT EXISTS (
    SELECT 1
    FROM employees e
    WHERE e.cognito_sub = v.cognito_sub
       OR e.email = v.email
);
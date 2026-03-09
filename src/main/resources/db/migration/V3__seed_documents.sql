INSERT INTO documents (
    id,
    document_number,
    title,
    summary,
    type_id,
    department_id,
    status_id,
    owner_id,
    created_by,
    updated_by,
    next_review_at,
    created_at,
    updated_at
)
VALUES
    (
        'b87c5a5f-7cd2-4f60-a62b-6d0956d98d70',
        'POL-001',
        'Quality Policy',
        'Defines quality commitments and management direction.',
        (SELECT id FROM document_types WHERE name = 'Policy'),
        (SELECT id FROM departments WHERE name = 'Compliance'),
        (SELECT id FROM document_statuses WHERE name = 'Active'),
        'user-compliance-lead',
        'seed-system',
        'seed-system',
        NOW() + INTERVAL '12 months',
        NOW(),
        NOW()
    ),
    (
        '95b3f1e8-d0d5-4310-8fb5-582f057dc6a8',
        'PRC-012',
        'Risk Management Procedure',
        'Procedure for identifying, assessing, and treating operational risk.',
        (SELECT id FROM document_types WHERE name = 'Procedure'),
        (SELECT id FROM departments WHERE name = 'Risk'),
        (SELECT id FROM document_statuses WHERE name = 'Pending Approval'),
        'user-risk-manager',
        'seed-system',
        'seed-system',
        NOW() + INTERVAL '6 months',
        NOW(),
        NOW()
    ),
    (
        'd15cadf4-d6f0-44cf-b6c9-59ea43ce9bc4',
        'PRC-020',
        'Incident Reporting Procedure',
        'Procedure for reporting, triaging, and closing incidents.',
        (SELECT id FROM document_types WHERE name = 'Procedure'),
        (SELECT id FROM departments WHERE name = 'Operations'),
        (SELECT id FROM document_statuses WHERE name = 'Draft'),
        'user-ops-lead',
        'seed-system',
        'seed-system',
        NOW() + INTERVAL '6 months',
        NOW(),
        NOW()
    );

INSERT INTO document_versions (
    document_id,
    version_number,
    s3_key,
    file_name,
    file_size,
    mime_type,
    uploaded_by,
    uploaded_at
)
VALUES
    (
        'b87c5a5f-7cd2-4f60-a62b-6d0956d98d70',
        1,
        'documents/b87c5a5f-7cd2-4f60-a62b-6d0956d98d70/v1/quality-policy.pdf',
        'quality-policy.pdf',
        314572,
        'application/pdf',
        'seed-system',
        NOW()
    ),
    (
        '95b3f1e8-d0d5-4310-8fb5-582f057dc6a8',
        1,
        'documents/95b3f1e8-d0d5-4310-8fb5-582f057dc6a8/v1/risk-management-procedure.pdf',
        'risk-management-procedure.pdf',
        262144,
        'application/pdf',
        'seed-system',
        NOW()
    );

INSERT INTO document_clause_links (document_id, clause_id) VALUES
    ('b87c5a5f-7cd2-4f60-a62b-6d0956d98d70', (SELECT id FROM clauses WHERE code = '5.2')),
    ('95b3f1e8-d0d5-4310-8fb5-582f057dc6a8', (SELECT id FROM clauses WHERE code = '6.1')),
    ('95b3f1e8-d0d5-4310-8fb5-582f057dc6a8', (SELECT id FROM clauses WHERE code = '7.5')),
    ('d15cadf4-d6f0-44cf-b6c9-59ea43ce9bc4', (SELECT id FROM clauses WHERE code = '7.5'));

INSERT INTO document_reviews (document_id, reviewer_id, due_date, review_notes)
VALUES
    ('b87c5a5f-7cd2-4f60-a62b-6d0956d98d70', 'user-qms-auditor', CURRENT_DATE + INTERVAL '45 days', 'Annual review cycle'),
    ('95b3f1e8-d0d5-4310-8fb5-582f057dc6a8', 'user-risk-committee', CURRENT_DATE + INTERVAL '30 days', 'Approval board review');


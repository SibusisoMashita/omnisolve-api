-- ============================================================================
-- Seed contractor document types and fix contractor documents
-- ============================================================================

INSERT INTO contractor_document_types (name, description, requires_expiry) VALUES
    ('Insurance Certificate',      'Public liability insurance certificate',                    true),
    ('COIDA Letter',               'Compensation for Occupational Injuries and Diseases Act letter', true),
    ('Safety File',                'Health and safety compliance file',                         false),
    ('Tax Clearance Certificate',  'SARS tax clearance certificate',                            true),
    ('BEE Certificate',            'Broad-Based Black Economic Empowerment certificate',        true);

-- Re-run document inserts now that document types exist
-- (V2 inserts returned 0 rows because contractor_document_types was empty at that time)

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/insurance.pdf',
           'insurance.pdf',
           CURRENT_DATE - INTERVAL '30 days',
           CURRENT_DATE + INTERVAL '11 months',
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'Insurance Certificate'
    WHERE c.name = 'Alpha Electrical';

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/coida.pdf',
           'coida.pdf',
           CURRENT_DATE - INTERVAL '20 days',
           CURRENT_DATE + INTERVAL '10 months',
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'COIDA Letter'
    WHERE c.name = 'Alpha Electrical';

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/safety_file.pdf',
           'safety_file.pdf',
           CURRENT_DATE - INTERVAL '6 months',
           NULL,
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'Safety File'
    WHERE c.name = 'Alpha Electrical';

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/tax_clearance.pdf',
           'tax_clearance.pdf',
           CURRENT_DATE - INTERVAL '2 months',
           CURRENT_DATE + INTERVAL '10 months',
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'Tax Clearance Certificate'
    WHERE c.name = 'Alpha Electrical';

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/bee_certificate.pdf',
           'bee_certificate.pdf',
           CURRENT_DATE - INTERVAL '4 months',
           CURRENT_DATE + INTERVAL '8 months',
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'BEE Certificate'
    WHERE c.name = 'Alpha Electrical';

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/insurance.pdf',
           'insurance.pdf',
           CURRENT_DATE - INTERVAL '11 months',
           CURRENT_DATE + INTERVAL '10 days',
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'Insurance Certificate'
    WHERE c.name = 'Bravo Civils';

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/coida.pdf',
           'coida.pdf',
           CURRENT_DATE - INTERVAL '3 months',
           CURRENT_DATE + INTERVAL '8 months',
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'COIDA Letter'
    WHERE c.name = 'Bravo Civils';

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/safety_file.pdf',
           'safety_file.pdf',
           CURRENT_DATE - INTERVAL '1 month',
           NULL,
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'Safety File'
    WHERE c.name = 'Bravo Civils';

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/tax_clearance.pdf',
           'tax_clearance.pdf',
           CURRENT_DATE - INTERVAL '5 months',
           CURRENT_DATE + INTERVAL '7 months',
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'Tax Clearance Certificate'
    WHERE c.name = 'Bravo Civils';

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/insurance.pdf',
           'insurance.pdf',
           CURRENT_DATE - INTERVAL '18 months',
           CURRENT_DATE - INTERVAL '2 months',
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'Insurance Certificate'
    WHERE c.name = 'Delta Maintenance';

INSERT INTO contractor_documents (contractor_id, document_type_id, s3_key, file_name, issued_at, expiry_date, uploaded_by)
    SELECT c.id, t.id,
           'contractors/' || c.id || '/safety_file.pdf',
           'safety_file.pdf',
           CURRENT_DATE - INTERVAL '2 years',
           NULL,
           'system'
    FROM contractors c
    JOIN contractor_document_types t ON t.name = 'Safety File'
    WHERE c.name = 'Delta Maintenance';

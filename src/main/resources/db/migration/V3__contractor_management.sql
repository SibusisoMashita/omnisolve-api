-- ============================================================================
-- OmniSolve Contractor Management Module
-- ============================================================================


-- ============================================================================
-- CONTRACTORS
-- External companies working for the organisation
-- ============================================================================

CREATE TABLE contractors (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                             organisation_id BIGINT NOT NULL
                                 REFERENCES organisations(id) ON DELETE CASCADE,

                             name VARCHAR(255) NOT NULL,
                             registration_number VARCHAR(100),

                             contact_person VARCHAR(255),
                             email VARCHAR(255),
                             phone VARCHAR(50),

                             status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                             UNIQUE (organisation_id, name)
);


-- ============================================================================
-- CONTRACTOR WORKERS
-- Individual workers belonging to a contractor company
-- ============================================================================

CREATE TABLE contractor_workers (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    contractor_id UUID NOT NULL
                                        REFERENCES contractors(id) ON DELETE CASCADE,

                                    first_name VARCHAR(100),
                                    last_name VARCHAR(100),

                                    id_number VARCHAR(50),
                                    phone VARCHAR(50),
                                    email VARCHAR(255),

                                    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================================
-- CONTRACTOR DOCUMENT TYPES
-- Defines compliance requirements (Insurance, COIDA, Safety File etc.)
-- ============================================================================

CREATE TABLE contractor_document_types (
                                           id BIGSERIAL PRIMARY KEY,

                                           name VARCHAR(255) NOT NULL UNIQUE,
                                           description VARCHAR(1000),

                                           requires_expiry BOOLEAN NOT NULL DEFAULT TRUE
);


-- ============================================================================
-- CONTRACTOR DOCUMENTS
-- Evidence uploaded for compliance
-- ============================================================================

CREATE TABLE contractor_documents (
                                      id BIGSERIAL PRIMARY KEY,

                                      contractor_id UUID NOT NULL
                                          REFERENCES contractors(id) ON DELETE CASCADE,

                                      document_type_id BIGINT
                                          REFERENCES contractor_document_types(id),

                                      s3_key VARCHAR(500) NOT NULL,
                                      file_name VARCHAR(255),
                                      file_size BIGINT,
                                      mime_type VARCHAR(100),

                                      issued_at DATE,
                                      expiry_date DATE,

                                      uploaded_by VARCHAR(255),
                                      uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================================
-- CONTRACTOR SITE ACCESS
-- Allows contractors to operate at specific sites
-- ============================================================================

CREATE TABLE contractor_sites (
                                  contractor_id UUID NOT NULL
                                      REFERENCES contractors(id) ON DELETE CASCADE,

                                  site_id BIGINT NOT NULL
                                      REFERENCES sites(id) ON DELETE CASCADE,

                                  PRIMARY KEY (contractor_id, site_id)
);


-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_contractors_org
    ON contractors(organisation_id);

CREATE INDEX idx_contractor_workers_contractor
    ON contractor_workers(contractor_id);

CREATE INDEX idx_contractor_documents_contractor
    ON contractor_documents(contractor_id);

CREATE INDEX idx_contractor_documents_type
    ON contractor_documents(document_type_id);

CREATE INDEX idx_contractor_sites_site
    ON contractor_sites(site_id);

-- ============================================================================
-- CONTRACTORS
-- ============================================================================

INSERT INTO contractors (
    organisation_id,
    name,
    registration_number,
    contact_person,
    email,
    phone
)
VALUES
    (1, 'ABC Electrical', 'REG-458221', 'John Mokoena', 'john@abcelectrical.co.za', '+27 82 555 1122'),
    (1, 'Mega Civils', 'REG-991244', 'Sarah Naidoo', 'sarah@megacivils.co.za', '+27 83 442 3344');


-- ============================================================================
-- CONTRACTOR COMPLIANCE DEMO DATA
-- Creates realistic compliance dashboard scenarios
-- ============================================================================


-- ============================================================================
-- CONTRACTORS
-- ============================================================================

INSERT INTO contractors (
    organisation_id,
    name,
    registration_number,
    contact_person,
    email,
    phone
)
VALUES
    (1,'Alpha Electrical','REG-10001','Peter Mokoena','peter@alphaelec.co.za','+27 82 555 1111'),
    (1,'Bravo Civils','REG-10002','Lisa Naidoo','lisa@bravocivils.co.za','+27 82 555 2222'),
    (1,'Delta Maintenance','REG-10003','Ahmed Patel','ahmed@deltamaint.co.za','+27 82 555 3333');


-- ============================================================================
-- SITE ACCESS
-- ============================================================================

INSERT INTO contractor_sites (contractor_id, site_id)
SELECT id,1 FROM contractors;


-- ============================================================================
-- CONTRACTOR WORKERS
-- ============================================================================

INSERT INTO contractor_workers (
    contractor_id,
    first_name,
    last_name,
    phone,
    email
)
SELECT id,'James','Nkosi','+27 82 111 1111','james@alphaelec.co.za'
FROM contractors WHERE name='Alpha Electrical';

INSERT INTO contractor_workers (
    contractor_id,
    first_name,
    last_name,
    phone,
    email
)
SELECT id,'Thabo','Zulu','+27 82 222 2222','thabo@bravocivils.co.za'
FROM contractors WHERE name='Bravo Civils';

INSERT INTO contractor_workers (
    contractor_id,
    first_name,
    last_name,
    phone,
    email
)
SELECT id,'Maria','Singh','+27 82 333 3333','maria@deltamaint.co.za'
FROM contractors WHERE name='Delta Maintenance';


-- ============================================================================
-- ALPHA ELECTRICAL (FULLY COMPLIANT)
-- ============================================================================

INSERT INTO contractor_documents (
    contractor_id,
    document_type_id,
    s3_key,
    file_name,
    issued_at,
    expiry_date,
    uploaded_by
)
SELECT c.id,t.id,
       'contractors/'||c.id||'/insurance.pdf',
       'insurance.pdf',
       CURRENT_DATE - INTERVAL '30 days',
    CURRENT_DATE + INTERVAL '11 months',
    'system'
FROM contractors c
    JOIN contractor_document_types t
ON t.name='Insurance Certificate'
WHERE c.name='Alpha Electrical';

INSERT INTO contractor_documents (
    contractor_id,
    document_type_id,
    s3_key,
    file_name,
    issued_at,
    expiry_date,
    uploaded_by
)
SELECT c.id,t.id,
       'contractors/'||c.id||'/coida.pdf',
       'coida.pdf',
       CURRENT_DATE - INTERVAL '20 days',
    CURRENT_DATE + INTERVAL '10 months',
    'system'
FROM contractors c
    JOIN contractor_document_types t
ON t.name='COIDA Letter'
WHERE c.name='Alpha Electrical';



-- ============================================================================
-- BRAVO CIVILS (EXPIRING SOON)
-- ============================================================================

INSERT INTO contractor_documents (
    contractor_id,
    document_type_id,
    s3_key,
    file_name,
    issued_at,
    expiry_date,
    uploaded_by
)
SELECT c.id,t.id,
       'contractors/'||c.id||'/insurance.pdf',
       'insurance.pdf',
       CURRENT_DATE - INTERVAL '11 months',
    CURRENT_DATE + INTERVAL '10 days',
    'system'
FROM contractors c
    JOIN contractor_document_types t
ON t.name='Insurance Certificate'
WHERE c.name='Bravo Civils';

INSERT INTO contractor_documents (
    contractor_id,
    document_type_id,
    s3_key,
    file_name,
    issued_at,
    expiry_date,
    uploaded_by
)
SELECT c.id,t.id,
       'contractors/'||c.id||'/coida.pdf',
       'coida.pdf',
       CURRENT_DATE - INTERVAL '3 months',
    CURRENT_DATE + INTERVAL '8 months',
    'system'
FROM contractors c
    JOIN contractor_document_types t
ON t.name='COIDA Letter'
WHERE c.name='Bravo Civils';



-- ============================================================================
-- DELTA MAINTENANCE (NON COMPLIANT)
-- ============================================================================

INSERT INTO contractor_documents (
    contractor_id,
    document_type_id,
    s3_key,
    file_name,
    issued_at,
    expiry_date,
    uploaded_by
)
SELECT c.id,t.id,
       'contractors/'||c.id||'/insurance.pdf',
       'insurance.pdf',
       CURRENT_DATE - INTERVAL '18 months',
    CURRENT_DATE - INTERVAL '2 months',
    'system'
FROM contractors c
    JOIN contractor_document_types t
ON t.name='Insurance Certificate'
WHERE c.name='Delta Maintenance';
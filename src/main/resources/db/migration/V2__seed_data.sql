-- ============================================================================
-- OmniSolve Seed Data
-- ============================================================================
-- Insert-only reference data, lookup values, and default system records.
-- ============================================================================

-- ============================================================================
-- CORE PLATFORM
-- departments
-- permissions
-- demo organisation
-- sites
-- roles
-- role_permissions
-- employees
-- ============================================================================

INSERT INTO departments (name, description) VALUES
    ('Operations', 'Operational controls and procedures'),
    ('Compliance', 'Compliance and regulatory management'),
    ('Risk', 'Enterprise risk oversight'),
    ('HR', 'People and talent management'),
    ('Finance', 'Financial controls and reporting'),
    ('Quality', 'Quality assurance and control'),
    ('Safety', 'Health and safety management'),
    ('Environmental', 'Environmental management');

INSERT INTO permissions (code, name, description) VALUES
    ('view_dashboard', 'View Dashboard', 'Access dashboard overview'),
    ('manage_documents', 'Manage Documents', 'Create and manage documents'),
    ('manage_employees', 'Manage Employees', 'Create and manage employees'),
    ('manage_risks', 'Manage Risks', 'Manage enterprise risks'),
    ('manage_audits', 'Manage Audits', 'Create and manage audits'),
    ('manage_incidents', 'Manage Incidents', 'Create and manage incidents'),
    ('view_reports', 'View Reports', 'Access reporting dashboards'),
    ('manage_settings', 'Manage Settings', 'Configure system settings');

INSERT INTO organisations (name, created_at, updated_at)
    VALUES ('OmniSolve Demo Organisation', NOW(), NOW());

INSERT INTO sites (organisation_id, name, created_at, updated_at)
    VALUES (
    (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation'),
    'Head Office',
    NOW(),
    NOW()
);

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
    ('Quality Manager', 'Manage quality documents and audits'),
    ('Safety Manager', 'Manage safety incidents and risks'),
    ('Environmental Manager', 'Manage environmental aspects and compliance'),
    ('Supervisor', 'View dashboard, manage risks, and view reports'),
    ('Employee', 'Basic access to dashboard and reports')
    ) AS role_data(name, description)
    CROSS JOIN (
    SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation'
    ) AS org;

INSERT INTO role_permissions (role_id, permission_id)
    SELECT
    r.id AS role_id,
    p.id AS permission_id
    FROM roles r
    CROSS JOIN permissions p
    WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
    AND r.name = 'Administrator';

INSERT INTO role_permissions (role_id, permission_id)
    SELECT
    r.id AS role_id,
    p.id AS permission_id
    FROM roles r
    CROSS JOIN permissions p
    WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
    AND r.name = 'Quality Manager'
    AND p.code IN ('view_dashboard', 'manage_documents', 'manage_audits', 'view_reports');

INSERT INTO role_permissions (role_id, permission_id)
    SELECT
    r.id AS role_id,
    p.id AS permission_id
    FROM roles r
    CROSS JOIN permissions p
    WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
    AND r.name = 'Safety Manager'
    AND p.code IN ('view_dashboard', 'manage_incidents', 'manage_risks', 'manage_audits', 'view_reports');

INSERT INTO role_permissions (role_id, permission_id)
    SELECT
    r.id AS role_id,
    p.id AS permission_id
    FROM roles r
    CROSS JOIN permissions p
    WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
    AND r.name = 'Environmental Manager'
    AND p.code IN ('view_dashboard', 'manage_documents', 'manage_incidents', 'manage_risks', 'view_reports');

INSERT INTO role_permissions (role_id, permission_id)
    SELECT
    r.id AS role_id,
    p.id AS permission_id
    FROM roles r
    CROSS JOIN permissions p
    WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
    AND r.name = 'Supervisor'
    AND p.code IN ('view_dashboard', 'manage_risks', 'view_reports');

INSERT INTO role_permissions (role_id, permission_id)
    SELECT
    r.id AS role_id,
    p.id AS permission_id
    FROM roles r
    CROSS JOIN permissions p
    WHERE r.organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')
    AND r.name = 'Employee'
    AND p.code IN ('view_dashboard', 'view_reports');

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


-- ============================================================================
-- COMPLIANCE FRAMEWORK
-- standards
-- clauses
-- ============================================================================

INSERT INTO standards (code, name, description, version, published_date) VALUES
    ('ISO-9001', 'ISO 9001 - Quality Management Systems',
    'Requirements for a quality management system where an organization needs to demonstrate its ability to consistently provide products and services that meet customer and applicable statutory and regulatory requirements.',
    '2015', '2015-09-15'),

    ('ISO-14001', 'ISO 14001 - Environmental Management Systems',
    'Requirements for an environmental management system that an organization can use to enhance its environmental performance.',
    '2015', '2015-09-15'),

    ('ISO-45001', 'ISO 45001 - Occupational Health and Safety Management Systems',
    'Requirements for an occupational health and safety (OH&S) management system, with guidance for its use, to enable an organization to proactively improve its OH&S performance.',
    '2018', '2018-03-12');

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '4', 'Context of the Organisation',
    'Understanding the organisation, its context, and interested parties.', NULL, 1, 4),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '5', 'Leadership',
    'Leadership commitment, policy establishment, and organisational roles.', NULL, 1, 5),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '6', 'Planning',
    'Planning actions to address risks and opportunities and setting objectives.', NULL, 1, 6),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '7', 'Support',
    'Support processes including resources, competence, awareness, communication, and documented information.', NULL, 1, 7),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '8', 'Operation',
    'Operational planning and control of processes delivering products and services.', NULL, 1, 8),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '9', 'Performance Evaluation',
    'Monitoring, measurement, analysis, evaluation, internal audit, and management review.', NULL, 1, 9),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '10', 'Improvement',
    'Nonconformity, corrective action, and continual improvement of the management system.', NULL, 1, 10);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '4.1', 'Understanding the organisation and its context',
    'Determine external and internal issues relevant to purpose and strategic direction.', '4', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '4.2', 'Understanding the needs and expectations of interested parties',
    'Determine interested parties and their requirements relevant to the QMS.', '4', 2, 2),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '4.3', 'Determining the scope of the quality management system',
    'Determine boundaries and applicability of the QMS to establish its scope.', '4', 2, 3),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '4.4', 'Quality management system and its processes',
    'Establish, implement, maintain and continually improve the QMS and its processes.', '4', 2, 4);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '5.1', 'Leadership and commitment',
    'Top management demonstrates leadership and commitment to the QMS.', '5', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '5.2', 'Policy',
    'Establish, implement and maintain a quality policy.', '5', 2, 2),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '5.3', 'Organizational roles, responsibilities and authorities',
    'Ensure responsibilities and authorities for relevant roles are assigned and communicated.', '5', 2, 3);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '6.1', 'Actions to address risks and opportunities',
    'Determine risks and opportunities to ensure QMS achieves intended results.', '6', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '6.2', 'Quality objectives and planning to achieve them',
    'Establish quality objectives at relevant functions, levels and processes.', '6', 2, 2),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '6.3', 'Planning of changes',
    'When changes to the QMS are determined, they shall be carried out in a planned manner.', '6', 2, 3);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '7.1', 'Resources',
    'Determine and provide resources needed for establishment, implementation, maintenance and continual improvement of the QMS.', '7', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '7.2', 'Competence',
    'Determine necessary competence of persons doing work under its control that affects performance and effectiveness of the QMS.', '7', 2, 2),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '7.3', 'Awareness',
    'Ensure persons doing work under the organization''s control are aware of the quality policy, relevant quality objectives, and their contribution to the effectiveness of the QMS.', '7', 2, 3),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '7.4', 'Communication',
    'Determine internal and external communications relevant to the QMS.', '7', 2, 4),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '7.5', 'Documented information',
    'The QMS shall include documented information required by this standard and determined by the organization as necessary for the effectiveness of the QMS.', '7', 2, 5);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '8.1', 'Operational planning and control',
    'Plan, implement and control processes needed to meet requirements for provision of products and services.', '8', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '8.2', 'Requirements for products and services',
    'Determine, review and meet requirements for products and services.', '8', 2, 2),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '8.3', 'Design and development of products and services',
    'Establish, implement and maintain a design and development process.', '8', 2, 3),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '8.4', 'Control of externally provided processes, products and services',
    'Ensure externally provided processes, products and services conform to requirements.', '8', 2, 4),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '8.5', 'Production and service provision',
    'Implement production and service provision under controlled conditions.', '8', 2, 5),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '8.6', 'Release of products and services',
    'Implement planned arrangements to verify that product and service requirements have been met.', '8', 2, 6),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '8.7', 'Control of nonconforming outputs',
    'Ensure outputs that do not conform to requirements are identified and controlled.', '8', 2, 7);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '9.1', 'Monitoring, measurement, analysis and evaluation',
    'Determine what needs to be monitored and measured, methods, when to perform, and when to analyze and evaluate results.', '9', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '9.2', 'Internal audit',
    'Conduct internal audits at planned intervals to provide information on whether the QMS conforms to requirements and is effectively implemented and maintained.', '9', 2, 2),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '9.3', 'Management review',
    'Top management reviews the QMS at planned intervals to ensure its continuing suitability, adequacy, effectiveness and alignment with strategic direction.', '9', 2, 3);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '10.1', 'General',
    'Determine and select opportunities for improvement and implement necessary actions.', '10', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '10.2', 'Nonconformity and corrective action',
    'When a nonconformity occurs, react to it, evaluate the need for action, implement action needed, and review effectiveness of corrective action taken.', '10', 2, 2),

    ((SELECT id FROM standards WHERE code = 'ISO-9001'), '10.3', 'Continual improvement',
    'Continually improve the suitability, adequacy and effectiveness of the QMS.', '10', 2, 3);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '4', 'Context of the Organisation',
    'Understanding the organisation and its context, including environmental aspects.', NULL, 1, 4),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '5', 'Leadership',
    'Leadership and commitment to the environmental management system.', NULL, 1, 5),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '6', 'Planning',
    'Planning for environmental aspects, compliance obligations, and objectives.', NULL, 1, 6),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '7', 'Support',
    'Resources, competence, awareness, communication, and documented information.', NULL, 1, 7),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '8', 'Operation',
    'Operational planning and control for environmental management.', NULL, 1, 8),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '9', 'Performance Evaluation',
    'Monitoring, measurement, analysis, evaluation, audit, and management review.', NULL, 1, 9),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '10', 'Improvement',
    'Nonconformity, corrective action, and continual improvement.', NULL, 1, 10);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '4.1', 'Understanding the organisation and its context',
    'Determine external and internal issues relevant to purpose and affecting ability to achieve intended outcomes of EMS.', '4', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '4.3', 'Determining the scope of the environmental management system',
    'Determine boundaries and applicability of the EMS to establish its scope.', '4', 2, 3),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '6.1', 'Actions to address risks and opportunities',
    'Determine risks and opportunities related to environmental aspects, compliance obligations, and other issues.', '6', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '6.1.2', 'Environmental aspects',
    'Determine environmental aspects of activities, products and services that can be controlled and influenced.', '6.1', 3, 2),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '7.5', 'Documented information',
    'The EMS shall include documented information required by this standard and determined necessary for effectiveness.', '7', 2, 5),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '8.1', 'Operational planning and control',
    'Establish, implement, control and maintain processes needed to meet EMS requirements.', '8', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-14001'), '9.1', 'Monitoring, measurement, analysis and evaluation',
    'Determine what needs to be monitored and measured, including environmental performance.', '9', 2, 1);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '4', 'Context of the Organisation',
    'Understanding the organisation and its context for OH&S management.', NULL, 1, 4),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '5', 'Leadership and Worker Participation',
    'Leadership, commitment, policy, and consultation and participation of workers.', NULL, 1, 5),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '6', 'Planning',
    'Actions to address risks and opportunities, OH&S objectives and planning.', NULL, 1, 6),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '7', 'Support',
    'Resources, competence, awareness, communication, and documented information.', NULL, 1, 7),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '8', 'Operation',
    'Operational planning and control, emergency preparedness and response.', NULL, 1, 8),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '9', 'Performance Evaluation',
    'Monitoring, measurement, analysis, evaluation, audit, and management review.', NULL, 1, 9),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '10', 'Improvement',
    'Incident, nonconformity, corrective action, and continual improvement.', NULL, 1, 10);

INSERT INTO clauses (standard_id, code, title, description, parent_code, level, sort_order) VALUES
    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '4.1', 'Understanding the organisation and its context',
    'Determine external and internal issues relevant to purpose and affecting ability to achieve intended outcomes of OH&S MS.', '4', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '5.1', 'Leadership and commitment',
    'Top management demonstrates leadership and commitment to the OH&S management system.', '5', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '5.4', 'Consultation and participation of workers',
    'Establish, implement and maintain processes for consultation and participation of workers.', '5', 2, 4),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '6.1', 'Actions to address risks and opportunities',
    'Determine risks and opportunities that need to be addressed to ensure OH&S MS achieves intended outcomes.', '6', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '6.1.2', 'Hazard identification and assessment of risks and opportunities',
    'Establish, implement and maintain processes for hazard identification and assessment of OH&S risks.', '6.1', 3, 2),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '7.5', 'Documented information',
    'The OH&S MS shall include documented information required by this standard and determined necessary for effectiveness.', '7', 2, 5),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '8.1', 'Operational planning and control',
    'Plan, implement and control processes needed to meet OH&S MS requirements.', '8', 2, 1),

    ((SELECT id FROM standards WHERE code = 'ISO-45001'), '10.2', 'Incident, nonconformity and corrective action',
    'Establish, implement and maintain processes to manage incidents and nonconformities.', '10', 2, 2);


-- ============================================================================
-- DOCUMENT MANAGEMENT
-- document statuses
-- document types
-- documents
-- document clause links
-- ============================================================================

INSERT INTO document_statuses (name) VALUES
    ('Draft'),
    ('Pending Approval'),
    ('Active'),
    ('Superseded'),
    ('Archived');

INSERT INTO document_types (name, description, requires_clauses) VALUES
    ('Policy', 'High-level management policies', true),
    ('Procedure', 'Step-by-step operational procedures', true),
    ('Manual', 'Management system manuals', true),
    ('Work Instruction', 'Detailed task instructions', true),
    ('Form', 'Operational forms', false),
    ('Record', 'Evidence records', false),
    ('Checklist', 'Operational checklists', false);

INSERT INTO documents (
    organisation_id,
    document_number,
    title,
    summary,
    type_id,
    status_id,
    department_id,
    owner_id,
    created_by,
    next_review_at,
    created_at,
    updated_at
    )
    SELECT
    (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation'),
    v.document_number,
    v.title,
    v.summary,
    (SELECT id FROM document_types WHERE name = v.type_name),
    (SELECT id FROM document_statuses WHERE name = v.status_name),
    (SELECT id FROM departments WHERE name = v.department_name),
    v.owner_id,
    v.created_by,
    v.next_review_at::timestamptz,
    v.created_at::timestamptz,
    v.updated_at::timestamptz
    FROM (
    VALUES
    ('DOC-2024-001', 'Quality Management Policy',
    'Defines the organization''s commitment to quality, customer satisfaction, and continual improvement.',
    'Policy', 'Active', 'Quality',
    '84d8f448-a0b1-701a-4694-0fc8ac94a614', 'Sibusiso Mashita',
    '2025-01-15 00:00:00+00', '2024-01-15 09:00:00+00', '2024-01-15 09:00:00+00'),

    ('DOC-2024-002', 'Document Control Procedure',
    'Establishes the process for creating, reviewing, approving, and maintaining controlled documents.',
    'Procedure', 'Active', 'Quality',
    '44a844f8-1081-70c1-895f-fd8900537782', 'Lefa',
    '2025-02-01 00:00:00+00', '2024-02-01 10:00:00+00', '2024-02-01 10:00:00+00'),

    ('DOC-2024-003', 'Internal Audit Procedure',
    'Defines the process for planning, conducting, and reporting internal audits of the management system.',
    'Procedure', 'Active', 'Quality',
    '84d8f448-a0b1-701a-4694-0fc8ac94a614', 'Sibusiso Mashita',
    '2025-03-01 00:00:00+00', '2024-03-01 11:00:00+00', '2024-03-01 11:00:00+00'),

    ('DOC-2024-004', 'Corrective Action Procedure',
    'Establishes the process for identifying, investigating, and resolving nonconformities.',
    'Procedure', 'Pending Approval', 'Quality',
    'a4f81438-80e1-70fb-b051-d066017e27e4', 'Siphiwe',
    '2025-04-01 00:00:00+00', '2024-03-10 14:00:00+00', '2024-03-12 16:00:00+00'),

    ('DOC-2024-005', 'Risk Assessment Procedure',
    'Defines the methodology for identifying, analyzing, and evaluating risks and opportunities.',
    'Procedure', 'Draft', 'Risk',
    '44a844f8-1081-70c1-895f-fd8900537782', 'Lefa',
    NULL, '2024-03-11 09:00:00+00', '2024-03-11 09:00:00+00'),

    ('DOC-2024-006', 'Environmental Policy',
    'Defines the organization''s commitment to environmental protection and sustainable practices.',
    'Policy', 'Active', 'Environmental',
    '84d8f448-a0b1-701a-4694-0fc8ac94a614', 'Sibusiso Mashita',
    '2025-01-20 00:00:00+00', '2024-01-20 10:00:00+00', '2024-01-20 10:00:00+00'),

    ('DOC-2024-007', 'Health and Safety Policy',
    'Establishes the organization''s commitment to providing a safe and healthy workplace.',
    'Policy', 'Active', 'Safety',
    'a4f81438-80e1-70fb-b051-d066017e27e4', 'Siphiwe',
    '2025-01-25 00:00:00+00', '2024-01-25 11:00:00+00', '2024-01-25 11:00:00+00'),

    ('DOC-2024-008', 'Management Review Procedure',
    'Defines the process for conducting periodic management reviews of the management system.',
    'Procedure', 'Active', 'Quality',
    '84d8f448-a0b1-701a-4694-0fc8ac94a614', 'Sibusiso Mashita',
    '2024-12-01 00:00:00+00', '2024-02-15 13:00:00+00', '2024-02-15 13:00:00+00'),

    ('DOC-2024-009', 'Training and Competence Procedure',
    'Establishes the process for identifying training needs, providing training, and evaluating effectiveness.',
    'Procedure', 'Draft', 'HR',
    '44a844f8-1081-70c1-895f-fd8900537782', 'Lefa',
    NULL, '2024-03-12 15:00:00+00', '2024-03-12 15:00:00+00'),

    ('DOC-2024-010', 'Supplier Evaluation Procedure',
    'Defines the criteria and process for evaluating and monitoring external providers.',
    'Procedure', 'Active', 'Operations',
    'a4f81438-80e1-70fb-b051-d066017e27e4', 'Siphiwe',
    '2025-02-20 00:00:00+00', '2024-02-20 09:00:00+00', '2024-02-20 09:00:00+00')
    ) AS v(document_number, title, summary, type_name, status_name, department_name, owner_id, created_by, next_review_at, created_at, updated_at);

INSERT INTO document_clause_links (document_id, clause_id)
    SELECT
    d.id,
    c.id
    FROM documents d
    CROSS JOIN LATERAL (
    VALUES
    ('DOC-2024-001', '5.2'),  -- Quality Policy -> Policy clause
    ('DOC-2024-001', '5.1'),  -- Quality Policy -> Leadership
    ('DOC-2024-002', '7.5'),  -- Document Control -> Documented Information
    ('DOC-2024-003', '9.2'),  -- Internal Audit -> Internal Audit clause
    ('DOC-2024-004', '10.2'), -- Corrective Action -> Nonconformity and corrective action
    ('DOC-2024-005', '6.1'),  -- Risk Assessment -> Actions to address risks
    ('DOC-2024-008', '9.3'),  -- Management Review -> Management review clause
    ('DOC-2024-009', '7.2'),  -- Training -> Competence clause
    ('DOC-2024-010', '8.4')   -- Supplier Evaluation -> Control of externally provided
    ) AS links(doc_num, clause_code)
    JOIN clauses c ON c.code = links.clause_code AND c.standard_id = (SELECT id FROM standards WHERE code = 'ISO-9001')
    WHERE d.document_number = links.doc_num;


-- ============================================================================
-- INCIDENT MANAGEMENT
-- incident types
-- severity levels
-- incident statuses
-- incidents
-- incident investigations
-- incident actions
-- incident comments
-- ============================================================================

INSERT INTO incident_types (name, description) VALUES
    ('Injury', 'Workplace injury or health incident'),
    ('Environmental', 'Environmental spill or impact'),
    ('Quality', 'Product or service quality issue'),
    ('Security', 'Security breach or threat'),
    ('Near Miss', 'Incident that could have caused harm but did not'),
    ('Property Damage', 'Damage to equipment or facilities'),
    ('Process Failure', 'Failure of operational process');

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

INSERT INTO incidents (
    organisation_id,
    incident_number,
    title,
    description,
    type_id,
    severity_id,
    status_id,
    department_id,
    site_id,
    occurred_at,
    reported_by,
    created_at,
    updated_at
    )
    SELECT
    (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation'),
    'INC-2024-' || LPAD(v.seq::text, 3, '0'),
    v.title,
    v.description,
    (SELECT id FROM incident_types WHERE name = v.type_name),
    (SELECT id FROM incident_severities WHERE name = v.severity_name),
    (SELECT id FROM incident_statuses WHERE name = v.status_name),
    (SELECT id FROM departments WHERE name = v.department_name),
    (SELECT id FROM sites WHERE name = 'Head Office' AND organisation_id = (SELECT id FROM organisations WHERE name = 'OmniSolve Demo Organisation')),
    v.occurred_at::timestamptz,
    v.reported_by,
    v.created_at::timestamptz,
    v.updated_at::timestamptz
    FROM (
    VALUES
    (1, 'Equipment malfunction in production area',
    'CNC machine stopped unexpectedly during operation. No injuries reported but production was halted for 2 hours.',
    'Property Damage', 'Medium', 'Closed', 'Operations',
    '2024-01-15 10:30:00+00', 'John Smith', '2024-01-15 11:00:00+00', '2024-01-20 14:00:00+00'),

    (2, 'Near miss - forklift and pedestrian',
    'Forklift operator nearly collided with pedestrian in warehouse. Pedestrian was not in designated walkway.',
    'Near Miss', 'High', 'Closed', 'Operations',
    '2024-02-03 14:15:00+00', 'Sarah Johnson', '2024-02-03 14:30:00+00', '2024-02-10 16:00:00+00'),

    (3, 'Chemical spill in storage area',
    'Small hydraulic oil spill (approximately 2 liters) in maintenance storage. Contained and cleaned immediately.',
    'Environmental', 'Low', 'Closed', 'Operations',
    '2024-02-18 09:45:00+00', 'Mike Chen', '2024-02-18 10:00:00+00', '2024-02-19 12:00:00+00'),

    (4, 'Product quality defect - batch QA-2024-045',
    'Quality inspection identified dimensional defects in 15% of batch QA-2024-045. Root cause investigation in progress.',
    'Quality', 'High', 'Investigation', 'Quality',
    '2024-03-01 13:20:00+00', 'Lisa Anderson', '2024-03-01 14:00:00+00', '2024-03-05 10:00:00+00'),

    (5, 'Unauthorized access attempt to server room',
    'Security system logged unauthorized access attempt to server room. Access denied, no breach occurred.',
    'Security', 'Medium', 'Closed', 'Operations',
    '2024-03-08 22:15:00+00', 'Security System', '2024-03-09 08:00:00+00', '2024-03-12 16:00:00+00'),

    (6, 'Minor hand injury during assembly',
    'Employee sustained minor cut on hand while assembling component. First aid administered, no lost time.',
    'Injury', 'Low', 'Closed', 'Operations',
    '2024-03-10 11:30:00+00', 'David Martinez', '2024-03-10 11:45:00+00', '2024-03-11 14:00:00+00'),

    (7, 'Compressed air system failure',
    'Main compressed air system failed causing production line shutdown. Emergency repairs completed within 4 hours.',
    'Process Failure', 'Critical', 'Action Required', 'Operations',
    '2024-03-12 07:00:00+00', 'Tom Wilson', '2024-03-12 07:15:00+00', '2024-03-13 09:00:00+00')
    ) AS v(seq, title, description, type_name, severity_name, status_name, department_name, occurred_at, reported_by, created_at, updated_at);

INSERT INTO incident_investigations (
    incident_id,
    investigator_id,
    analysis_method,
    root_cause,
    findings,
    created_at
    )
    SELECT
    i.id,
    v.investigator_id,
    v.analysis_method,
    v.root_cause,
    v.findings,
    v.created_at::timestamptz
    FROM (
    VALUES
    ('INC-2024-001', '84d8f448-a0b1-701a-4694-0fc8ac94a614', '5 Whys',
    'Preventive maintenance schedule was not followed due to production pressure',
    'Investigation revealed that the CNC machine had exceeded its scheduled maintenance interval by 3 weeks. Maintenance logs showed the last service was delayed due to high production demands. The machine''s hydraulic system showed signs of wear that would have been detected during routine maintenance.',
    '2024-01-16 10:00:00+00'),

    ('INC-2024-002', '44a844f8-1081-70c1-895f-fd8900537782', 'Root Cause Analysis',
    'Inadequate pedestrian safety controls and lack of awareness',
    'Analysis identified two contributing factors: 1) Pedestrian walkway markings had faded and were not clearly visible, 2) Forklift operator was distracted by radio communication. Both parties acknowledged the near miss could have been prevented with better adherence to safety protocols.',
    '2024-02-04 09:00:00+00'),

    ('INC-2024-003', 'a4f81438-80e1-70fb-b051-d066017e27e4', 'Incident Investigation',
    'Improper storage container seal',
    'The hydraulic oil container had a damaged seal that was not identified during the last inspection. The spill occurred when the container was moved, causing oil to leak. Spill response procedures were followed correctly, and environmental impact was minimal.',
    '2024-02-18 14:00:00+00'),

    ('INC-2024-005', '84d8f448-a0b1-701a-4694-0fc8ac94a614', 'Security Audit',
    'Former employee attempted access with expired credentials',
    'Security logs showed access attempt using credentials of an employee who left the company 2 weeks prior. The access control system correctly denied entry. Investigation revealed a gap in the offboarding process where physical access cards were collected but system credentials were not immediately disabled.',
    '2024-03-09 10:00:00+00'),

    ('INC-2024-006', '44a844f8-1081-70c1-895f-fd8900537782', 'Incident Report',
    'Sharp edge on component not properly deburred',
    'The component had a sharp edge that should have been removed during the deburring process. Quality control inspection missed this defect. Employee was wearing appropriate PPE which prevented a more serious injury.',
    '2024-03-10 15:00:00+00')
    ) AS v(incident_number, investigator_id, analysis_method, root_cause, findings, created_at)
    JOIN incidents i ON i.incident_number = v.incident_number;

INSERT INTO incident_actions (
    incident_id,
    title,
    description,
    assigned_to,
    due_date,
    status,
    completed_at,
    created_at
    )
    SELECT
    i.id,
    v.title,
    v.description,
    v.assigned_to,
    v.due_date::date,
    v.status,
    v.completed_at::timestamptz,
    v.created_at::timestamptz
    FROM (
    VALUES
    ('INC-2024-001', 'Update preventive maintenance schedule',
    'Review and update the preventive maintenance schedule to include buffer time and escalation procedures when maintenance cannot be performed on schedule.',
    '84d8f448-a0b1-701a-4694-0fc8ac94a614', '2024-02-01', 'Completed', '2024-01-28 16:00:00+00', '2024-01-16 11:00:00+00'),

    ('INC-2024-001', 'Conduct maintenance training',
    'Provide additional training to production supervisors on the importance of adhering to maintenance schedules.',
    'a4f81438-80e1-70fb-b051-d066017e27e4', '2024-02-15', 'Completed', '2024-02-12 14:00:00+00', '2024-01-16 11:00:00+00'),

    ('INC-2024-002', 'Repaint pedestrian walkways',
    'Repaint all pedestrian walkway markings in warehouse and production areas with high-visibility paint.',
    '44a844f8-1081-70c1-895f-fd8900537782', '2024-02-20', 'Completed', '2024-02-18 17:00:00+00', '2024-02-04 10:00:00+00'),

    ('INC-2024-002', 'Implement forklift safety refresher',
    'Conduct mandatory safety refresher training for all forklift operators focusing on pedestrian awareness.',
    'a4f81438-80e1-70fb-b051-d066017e27e4', '2024-03-01', 'Completed', '2024-02-28 16:00:00+00', '2024-02-04 10:00:00+00'),

    ('INC-2024-003', 'Inspect all chemical storage containers',
    'Conduct comprehensive inspection of all chemical storage containers and replace any with damaged seals.',
    '84d8f448-a0b1-701a-4694-0fc8ac94a614', '2024-02-25', 'Completed', '2024-02-23 15:00:00+00', '2024-02-18 15:00:00+00'),

    ('INC-2024-004', 'Review quality control procedures',
    'Review and enhance quality control inspection procedures for dimensional accuracy.',
    '44a844f8-1081-70c1-895f-fd8900537782', '2024-03-20', 'In Progress', NULL, '2024-03-05 11:00:00+00'),

    ('INC-2024-005', 'Update offboarding checklist',
    'Update employee offboarding checklist to include immediate system access revocation.',
    'a4f81438-80e1-70fb-b051-d066017e27e4', '2024-03-15', 'Completed', '2024-03-14 12:00:00+00', '2024-03-09 11:00:00+00'),

    ('INC-2024-006', 'Enhance deburring quality checks',
    'Add specific sharp edge inspection to deburring quality control checklist.',
    '84d8f448-a0b1-701a-4694-0fc8ac94a614', '2024-03-18', 'Completed', '2024-03-17 14:00:00+00', '2024-03-10 16:00:00+00'),

    ('INC-2024-007', 'Schedule compressed air system overhaul',
    'Schedule comprehensive overhaul of compressed air system including all compressors, dryers, and distribution lines.',
    '44a844f8-1081-70c1-895f-fd8900537782', '2024-04-15', 'Pending', NULL, '2024-03-13 10:00:00+00'),

    ('INC-2024-007', 'Implement backup air supply',
    'Evaluate and implement backup compressed air supply to prevent future production shutdowns.',
    'a4f81438-80e1-70fb-b051-d066017e27e4', '2024-05-01', 'Pending', NULL, '2024-03-13 10:00:00+00')
    ) AS v(incident_number, title, description, assigned_to, due_date, status, completed_at, created_at)
    JOIN incidents i ON i.incident_number = v.incident_number;

INSERT INTO incident_comments (
    incident_id,
    comment,
    created_by,
    created_at
    )
    SELECT
    i.id,
    v.comment,
    v.created_by,
    v.created_at::timestamptz
    FROM (
    VALUES
    ('INC-2024-001', 'Production team notified of incident. Machine taken offline for inspection.',
    '84d8f448-a0b1-701a-4694-0fc8ac94a614', '2024-01-15 11:30:00+00'),
    ('INC-2024-001', 'Maintenance team completed emergency repairs. Full service scheduled for next week.',
    'a4f81438-80e1-70fb-b051-d066017e27e4', '2024-01-15 16:00:00+00'),
    ('INC-2024-001', 'Root cause analysis completed. Corrective actions assigned.',
    '84d8f448-a0b1-701a-4694-0fc8ac94a614', '2024-01-16 14:00:00+00'),

    ('INC-2024-002', 'Both parties interviewed. Statements collected.',
    '44a844f8-1081-70c1-895f-fd8900537782', '2024-02-03 16:00:00+00'),
    ('INC-2024-002', 'Safety committee reviewed incident. Recommendations documented.',
    '44a844f8-1081-70c1-895f-fd8900537782', '2024-02-05 10:00:00+00'),

    ('INC-2024-004', 'Quality team isolated affected batch. Customer notification prepared.',
    '44a844f8-1081-70c1-895f-fd8900537782', '2024-03-01 15:00:00+00'),
    ('INC-2024-004', 'Investigation ongoing. Additional samples being tested.',
    '44a844f8-1081-70c1-895f-fd8900537782', '2024-03-05 11:00:00+00'),

    ('INC-2024-007', 'Emergency response team activated. Production line 1 and 2 affected.',
    '84d8f448-a0b1-701a-4694-0fc8ac94a614', '2024-03-12 07:30:00+00'),
    ('INC-2024-007', 'Temporary repairs completed. System operational but monitoring required.',
    'a4f81438-80e1-70fb-b051-d066017e27e4', '2024-03-12 11:00:00+00')
    ) AS v(incident_number, comment, created_by, created_at)
    JOIN incidents i ON i.incident_number = v.incident_number;


-- ============================================================================
-- ASSURANCE / INSPECTIONS
-- inspection types
-- inspection severities
-- asset types
-- inspection checklists
-- inspection checklist items
-- ============================================================================

INSERT INTO inspection_types (code, name, description) VALUES
    ('INSPECTION', 'Inspection', 'Standard operational inspection'),
    ('AUDIT', 'Audit', 'Internal or external audit'),
    ('SAFETY_WALK', 'Safety Walk', 'Safety observation walk'),
    ('ENV_CHECK', 'Environmental Check', 'Environmental compliance inspection'),
    ('RISK_REVIEW', 'Risk Review', 'Risk assessment review');

INSERT INTO inspection_severities (name, level) VALUES
    ('Low', 1),
    ('Medium', 2),
    ('High', 3),
    ('Critical', 4);

INSERT INTO asset_types (name, description) VALUES
    ('Vehicle', 'Cars, trucks, vans and other motor vehicles'),
    ('Forklift', 'Forklifts and material handling equipment'),
    ('Fire Equipment', 'Fire extinguishers and suppression systems'),
    ('Electrical Equipment', 'Electrical panels and switchboards'),
    ('PPE', 'Personal protective equipment');

INSERT INTO inspection_checklists (name, description, asset_type_id) VALUES
    ('Vehicle Pre-Trip Inspection', 'Daily vehicle pre-trip inspection checklist', 1),
    ('Forklift Pre-Shift Inspection', 'Pre-shift forklift safety inspection', 2),
    ('Fire Extinguisher Monthly Check', 'Monthly fire extinguisher inspection', 3);

INSERT INTO inspection_checklist_items (checklist_id, title, description, sort_order) VALUES
    (1, 'Tyres', 'Check tyre pressure and condition', 1),
    (1, 'Lights', 'Check headlights, indicators and brake lights', 2),
    (1, 'Brakes', 'Check brake function', 3),
    (1, 'Fire Extinguisher Present', 'Confirm fire extinguisher is present', 4),
    (1, 'Oil Level', 'Check engine oil level', 5),
    (1, 'Windscreen', 'Check windscreen for cracks', 6),

    (2, 'Forks', 'Inspect forks for cracks or damage', 1),
    (2, 'Hydraulics', 'Check hydraulic system for leaks', 2),
    (2, 'Horn', 'Verify horn is audible', 3),
    (2, 'Brakes', 'Test service and parking brake', 4),
    (2, 'Tyres', 'Inspect tyre condition', 5),

    (3, 'Pressure Gauge', 'Pressure gauge in green zone', 1),
    (3, 'Safety Pin', 'Safety pin and seal intact', 2),
    (3, 'Physical Damage', 'Inspect extinguisher body', 3),
    (3, 'Label Legible', 'Operating instructions visible', 4);


-- ============================================================================
-- CONTRACTOR MANAGEMENT
-- contractors
-- contractor document types
-- contractor site access
-- contractor workers
-- contractor documents
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

INSERT INTO contractor_sites (contractor_id, site_id)
    SELECT id,1 FROM contractors;

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


-- ============================================================================
-- RISK MANAGEMENT
-- risk categories
-- severity levels
-- likelihood levels
-- ============================================================================

INSERT INTO risk_categories (name, description) VALUES
    ('Operational', 'Operational process risks'),
    ('Safety', 'Workplace health and safety risks'),
    ('Environmental', 'Environmental impact risks'),
    ('Compliance', 'Legal and regulatory compliance risks'),
    ('Financial', 'Financial or business continuity risks');

INSERT INTO risk_severities (name, level) VALUES
    ('Low',1),
    ('Medium',2),
    ('High',3),
    ('Critical',4);

INSERT INTO risk_likelihoods (name, level) VALUES
    ('Rare',1),
    ('Possible',2),
    ('Likely',3),
    ('Almost Certain',4);


-- ============================================================================
-- OBJECTIVES MANAGEMENT
-- no default seed data
-- ============================================================================

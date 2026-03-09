INSERT INTO document_statuses (name) VALUES
    ('Draft'),
    ('Pending Approval'),
    ('Active'),
    ('Superseded'),
    ('Archived');

INSERT INTO document_types (name, description, requires_clauses)
VALUES
    ('Policy', 'High-level management policies', true),
    ('Procedure', 'Step-by-step operational procedures', true),
    ('Manual', 'Management system manuals', true),
    ('Work Instruction', 'Detailed task instructions', true),
    ('Form', 'Operational forms', false),
    ('Record', 'Evidence records', false),
    ('Checklist', 'Operational checklists', false);

INSERT INTO departments (name, description) VALUES
    ('Operations', 'Operational controls and procedures'),
    ('Compliance', 'Compliance and regulatory management'),
    ('Risk', 'Enterprise risk oversight'),
    ('HR', 'People and talent management'),
    ('Finance', 'Financial controls and reporting');

INSERT INTO clauses (code, title, description) VALUES
    ('4.4', 'Quality management system and its processes', 'Define and control QMS processes and interactions.'),
    ('5.2', 'Quality policy', 'Establish, communicate, and maintain the quality policy.'),
    ('6.1', 'Actions to address risks and opportunities', 'Plan actions to manage risk and opportunities.'),
    ('7.5', 'Documented information', 'Create, update, and control documented information.'),
    ('9.2', 'Internal audit', 'Conduct internal audits to verify QMS effectiveness.');


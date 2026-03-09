INSERT INTO document_statuses (name) VALUES
    ('Draft'),
    ('Pending Approval'),
    ('Active'),
    ('Superseded'),
    ('Archived');

INSERT INTO document_types (name, description) VALUES
    ('Policy', 'High-level management intentions and rules'),
    ('Procedure', 'Detailed steps to execute a process'),
    ('Work Instruction', 'Task-level operational instruction'),
    ('Form', 'Template used to capture records'),
    ('Record', 'Evidence of performed activities');

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


CREATE TABLE actions.script_contingency_list (
    name text,
    script text,
    PRIMARY KEY (name)
);

CREATE TABLE actions.filters_contingency_list (
    name text,
    equipmentId text,
    equipmentName text,
    equipmentType text,
    nominalVoltage float,
    nominalVoltageOperator text,
    PRIMARY KEY (name)
);


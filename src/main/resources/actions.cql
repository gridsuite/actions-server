CREATE TABLE actions.script_contingency_list (
    name text,
    script text,
    PRIMARY KEY (name)
);

CREATE TABLE actions.filter_contingency_list (
    name text,
    equipmentId text,
    equipmentName text,
    equipmentType list<text>,
    nominalVoltage text,
    nominalVoltageOperator text,
    PRIMARY KEY (name)
);


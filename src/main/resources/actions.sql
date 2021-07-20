
    create table filters_contingency_list (
       id uuid not null,
        creationDate timestamp,
        description varchar(255),
        modificationDate timestamp,
        name varchar(255),
        equipmentId varchar(255),
        equipmentName varchar(255),
        equipmentType varchar(255),
        nominalVoltage float8,
        nominalVoltageOperator varchar(255),
        primary key (id)
    );

    create table FiltersContingencyListEntity_countries (
       FiltersContingencyListEntity_id uuid not null,
        country varchar(255)
    );

    create table script_contingency_list (
       id uuid not null,
        creationDate timestamp,
        description varchar(255),
        modificationDate timestamp,
        name varchar(255),
        script TEXT,
        primary key (id)
    );
create index filter_contingency_list_name_idx on filters_contingency_list (name);
create index filtersContingencyListEntity_countries_idx on FiltersContingencyListEntity_countries (FiltersContingencyListEntity_id);
create index script_contingency_list_name_idx on script_contingency_list (name);

    alter table if exists FiltersContingencyListEntity_countries 
       add constraint filtersContingencyListEntity_countries_fk 
       foreign key (FiltersContingencyListEntity_id) 
       references filters_contingency_list;

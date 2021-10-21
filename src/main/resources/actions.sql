
    create table filters_contingency_list (
       id uuid not null,
        creationDate timestamp,
        modificationDate timestamp,
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
        modificationDate timestamp,
        script TEXT,
        primary key (id)
    );
create index filtersContingencyListEntity_countries_idx on FiltersContingencyListEntity_countries (FiltersContingencyListEntity_id);

    alter table if exists FiltersContingencyListEntity_countries 
       add constraint filtersContingencyListEntity_countries_fk 
       foreign key (FiltersContingencyListEntity_id) 
       references filters_contingency_list;

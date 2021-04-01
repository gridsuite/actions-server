
    create table filters_contingency_list (
       name varchar(255) not null,
        equipmentId varchar(255),
        equipmentName varchar(255),
        equipmentType varchar(255),
        nominalVoltage float8,
        nominalVoltageOperator varchar(255),
        primary key (name)
    );

    create table FiltersContingencyListEntity_countries (
       FiltersContingencyListEntity_name varchar(255) not null,
        country varchar(255)
    );

    create table script_contingency_list (
       name varchar(255) not null,
        script TEXT,
        primary key (name)
    );

    alter table if exists FiltersContingencyListEntity_countries 
       add constraint filtersContingencyListEntity_countries_fk 
       foreign key (FiltersContingencyListEntity_name) 
       references filters_contingency_list;

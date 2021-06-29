
    create table filters_contingency_list (
       idList uuid not null,
        equipmentId varchar(255),
        equipmentName varchar(255),
        equipmentType varchar(255),
        name varchar(255),
        nominalVoltage float8,
        nominalVoltageOperator varchar(255),
        primary key (idList)
    );

    create table FiltersContingencyListEntity_countries (
       FiltersContingencyListEntity_idList uuid not null,
        country varchar(255)
    );

    create table script_contingency_list (
       id uuid not null,
        name varchar(255),
        script TEXT,
        primary key (id)
    );
create index script_contingency_list_name_idx on filters_contingency_list (name);
create index filtersContingencyListEntity_countries_idx on FiltersContingencyListEntity_countries (FiltersContingencyListEntity_idList);
create index script_contingency_list_name_idx on script_contingency_list (name);

    alter table if exists FiltersContingencyListEntity_countries 
       add constraint filtersContingencyListEntity_countries_fk 
       foreign key (FiltersContingencyListEntity_idList) 
       references filters_contingency_list;

    create table filters_contingency_list (
       idList uuid not null,
        equipmentId varchar(255),
        equipmentName varchar(255),
        equipmentType varchar(255),
        name varchar(255),
        nominalVoltage float8,
        nominalVoltageOperator varchar(255),
        primary key (idList)
    );

    create table FiltersContingencyListEntity_countries (
       FiltersContingencyListEntity_idList uuid not null,
        country varchar(255)
    );

    create table script_contingency_list (
       id uuid not null,
        name varchar(255),
        script TEXT,
        primary key (id)
    );
create index script_contingency_list_name_idx on filters_contingency_list (name);
create index filtersContingencyListEntity_countries_idx on FiltersContingencyListEntity_countries (FiltersContingencyListEntity_idList);
create index script_contingency_list_name_idx on script_contingency_list (name);

    alter table if exists FiltersContingencyListEntity_countries 
       add constraint filtersContingencyListEntity_countries_fk 
       foreign key (FiltersContingencyListEntity_idList) 
       references filters_contingency_list;

/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.dto.FiltersContingencyListAttributes;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.collections4.SetUtils.emptyIfNull;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */
@Getter
@Table("filters_contingency_list")
@NoArgsConstructor
public class FiltersContingencyListEntity {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    private String name;

    private String equipmentId;

    private String equipmentName;

    private String equipmentType;

    private double nominalVoltage;

    private String nominalVoltageOperator;

    private Set<String> countries;

    public FiltersContingencyListEntity(String name, FiltersContingencyListAttributes filtersContingencyListAttributes) {
        this.name = name;
        this.equipmentId = filtersContingencyListAttributes.getEquipmentID();
        this.equipmentName = filtersContingencyListAttributes.getEquipmentName();
        this.equipmentType = filtersContingencyListAttributes.getEquipmentType();
        this.nominalVoltage = filtersContingencyListAttributes.getNominalVoltage();
        this.nominalVoltageOperator = filtersContingencyListAttributes.getNominalVoltageOperator();
        this.countries = new HashSet<>(emptyIfNull(filtersContingencyListAttributes.getCountries()));
    }
}

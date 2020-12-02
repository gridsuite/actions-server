/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.dto.FilterContingencyListAttributes;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

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

    private String nominalVoltage;

    private String nominalVoltageOperator;

    public FiltersContingencyListEntity(String name, FilterContingencyListAttributes filterContingencyListAttributes) {
        this.name = name;
        this.equipmentId = filterContingencyListAttributes.getEquipmentID();
        this.equipmentName = filterContingencyListAttributes.getEquipmentName();
        this.equipmentType = filterContingencyListAttributes.getEquipmentType();
        this.nominalVoltage = filterContingencyListAttributes.getNominalVoltage();
        this.nominalVoltageOperator = filterContingencyListAttributes.getNominalVoltageOperator();
    }
}

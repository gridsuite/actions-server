/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.dto.FilterContingencyListAttributes;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.List;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */
@Getter
@Table("filter_contingency_list")
@AllArgsConstructor
@NoArgsConstructor
public class FilterContingencyListEntity {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    private String name;

    private String equipmentId;

    private String equipmentName;

    private List<String> equipmentType;

    private String nominalVoltage;

    private String nominalVoltageOperator;

    public FilterContingencyListEntity(String name, FilterContingencyListAttributes filterContingencyListAttributes) {
        this.name = name;
        this.equipmentId = filterContingencyListAttributes.getEquipmentId();
        this.equipmentName = filterContingencyListAttributes.getEquipmentName();
        this.equipmentType = filterContingencyListAttributes.getEquipmentType();
        this.nominalVoltage = filterContingencyListAttributes.getNominalVoltage();
        this.nominalVoltageOperator = filterContingencyListAttributes.getNominalVoltageOperator();
    }
}

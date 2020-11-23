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
import org.gridsuite.actions.server.dto.GuiContingencyListAttributes;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.List;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */
@Getter
@Table("gui_contingency_list")
@AllArgsConstructor
@NoArgsConstructor
public class GuiContingencyListEntity {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    private String name;

    private String equipmentId;

    private String equipmentName;

    private List<String> equipmentType;

    private String nominalVoltage;

    private String nominalVoltageOperator;

    public GuiContingencyListEntity(String name, GuiContingencyListAttributes guiContingencyListAttributes) {
        this.name = name;
        this.equipmentId = guiContingencyListAttributes.getEquipmentId();
        this.equipmentName = guiContingencyListAttributes.getEquipmentName();
        this.equipmentType = guiContingencyListAttributes.getEquipmentType();
        this.nominalVoltage = guiContingencyListAttributes.getNominalVoltage();
        this.nominalVoltageOperator = guiContingencyListAttributes.getNominalVoltageOperator();
    }
}

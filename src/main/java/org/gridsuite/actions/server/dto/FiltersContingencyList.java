/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("filter contingency list")
public class FiltersContingencyList implements ContingencyList {

    @ApiModelProperty("List name")
    private String name;

    @ApiModelProperty("equipmentID")
    private String equipmentID;

    @ApiModelProperty("equipmentName")
    private String equipmentName;

    @ApiModelProperty("equipmentType")
    private String equipmentType;

    @ApiModelProperty("nominalVoltage")
    private String nominalVoltage;

    @ApiModelProperty("nominalVoltageOperator")
    private String nominalVoltageOperator;

    @Override
    public ContingencyListType getType() {
        return ContingencyListType.FILTERS;
    }
}

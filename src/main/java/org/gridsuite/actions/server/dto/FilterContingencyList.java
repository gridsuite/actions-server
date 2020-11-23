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

import java.util.List;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("filter contingency list")
public class FilterContingencyList implements ContingencyList {

    @ApiModelProperty("List name")
    private String name;

    @ApiModelProperty("equipmentId")
    private String equipmentId;

    @ApiModelProperty("equipmentName")
    private String equipmentName;

    @ApiModelProperty("equipmentType")
    private List<String> equipmentType;

    @ApiModelProperty("nominalVoltage")
    private String nominalVoltage;

    @ApiModelProperty("nominalVoltageOperator")
    private String nominalVoltageOperator;

}

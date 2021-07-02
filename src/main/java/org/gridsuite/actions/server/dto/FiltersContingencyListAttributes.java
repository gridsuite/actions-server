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
import lombok.Setter;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.util.Set;
import java.util.UUID;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("filter contingency list attributes")
public class FiltersContingencyListAttributes implements ContingencyList {

    @ApiModelProperty("list id")
    private UUID id;

    @ApiModelProperty("list name")
    private String name;

    @ApiModelProperty("Equipment ID")
    private String equipmentID;

    @ApiModelProperty("Equipment name")
    private String equipmentName;

    @ApiModelProperty("Equipment type")
    private String equipmentType;

    @ApiModelProperty("Nominal voltage")
    private double nominalVoltage;

    @ApiModelProperty("Nominal voltage operator")
    private String nominalVoltageOperator;

    @ApiModelProperty("Countries")
    private Set<String> countries;

    @ApiModelProperty("Description")
    private String description;

    @Override
    public ContingencyListType getType() {
        return ContingencyListType.FILTERS;
    }
}

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

import java.util.Date;
import java.util.UUID;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Contingency list attributes")
public class ContingencyListAttributes implements ContingencyList {

    @ApiModelProperty("list id")
    private UUID id;

    @ApiModelProperty("List name")
    private String name;

    @ApiModelProperty("List type")
    private ContingencyListType type;

    @ApiModelProperty("Creation Date")
    Date creationDate;

    @ApiModelProperty("Modification Date")
    Date modificationDate;

    @ApiModelProperty("Description")
    String description;
}

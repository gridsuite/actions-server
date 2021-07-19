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

import java.util.UUID;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("Script contingency list")
public class ScriptContingencyList implements ContingencyList {

    @ApiModelProperty("list id")
    private UUID id;

    @ApiModelProperty("List name")
    private String name;

    @ApiModelProperty("Script")
    private String script;

    @ApiModelProperty("Type")
    @Override
    public ContingencyListType getType() {
        return ContingencyListType.SCRIPT;
    }

    @ApiModelProperty("description")
    private String description;

}

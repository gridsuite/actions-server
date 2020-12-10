/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Jon Harper <jon.harper at rte-france.com>
 */

@Getter
@Setter
@NoArgsConstructor
@ApiModel("Contingency list renaming infos")
public class RenameContingencyListAttributes {

    @ApiModelProperty("New contingency list name")
    private String newContingencyListName;
}

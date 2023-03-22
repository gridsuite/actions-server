/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.contingency.list.ContingencyList;
import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer@rte-france.com>
 */
@Getter
@NoArgsConstructor
@Schema(description = "Id based contingency list")

public class IdBasedContingencyList extends AbstractContingencyList {

    @Schema(description = "Identifier list")
    private IdentifierContingencyList identifierContingencyList;

    public IdBasedContingencyList(UUID uuid, IdentifierContingencyList identifierContingencyList) {
        super(uuid);
        this.identifierContingencyList = identifierContingencyList;
    }

    @Schema(description = "Type")
    @Override
    public ContingencyListType getType() {
        return ContingencyListType.IDENTIFIERS;
    }

    @Override
    public ContingencyList toPowsyblContingencyList() {
        return identifierContingencyList;
    }
}

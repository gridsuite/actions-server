/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.contingency.list.ContingencyList;
import com.powsybl.iidm.network.Network;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Schema(description = "filter based contingency list")
public class FilterBasedContingencyList extends AbstractContingencyList {

    @Schema(description = "filters list")
    private List<FilterMetaData> filters;

    public FilterBasedContingencyList(UUID uuid, Instant date, List<FilterMetaData> filterList) {
        super(new ContingencyListMetadataImpl(uuid, ContingencyListType.FILTERS, date));
        this.filters = filterList;
    }

    @Override
    public ContingencyList toPowsyblContingencyList(Network network) {
        return null;
    }

    @Override
    public Map<String, Set<String>> getNotFoundElements(Network network) {
        return Map.of();
    }
}

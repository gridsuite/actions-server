/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.contingency.list.ContingencyList;
import com.powsybl.iidm.network.Network;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Getter
@NoArgsConstructor
public abstract class AbstractContingencyList implements PersistentContingencyList {

    private ContingencyListMetadataImpl metadata;

    protected AbstractContingencyList(ContingencyListMetadataImpl metadata) {
        this.metadata = metadata;
    }

    @Override public ContingencyListMetadata getMetadata() {
        return metadata;
    }

    public List<Contingency> getContingencies(Network network) {
        return toPowsyblContingencyList().getContingencies(network);
    }

    public ContingencyList toPowsyblContingencyList() {
        return null;
    }
}

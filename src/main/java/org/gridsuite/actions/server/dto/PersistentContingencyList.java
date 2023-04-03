package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.contingency.list.ContingencyList;

public interface PersistentContingencyList {
    ContingencyListMetadata getMetadata();

    ContingencyList toPowsyblContingencyList();
}

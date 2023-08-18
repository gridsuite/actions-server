/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import com.powsybl.contingency.contingency.list.identifier.IdBasedNetworkElementIdentifier;
import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifier;
import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifierList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.dto.IdBasedContingencyList;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer@rte-france.com>
 */
@NoArgsConstructor
@Getter
@Entity
@Table(name = "id_based_contingency_list")
public class IdBasedContingencyListEntity extends AbstractContingencyEntity {

    @OneToMany(cascade = CascadeType.ALL)
    private List<IdentifierListEntity> identifiersListEntities;

    public IdBasedContingencyListEntity(IdBasedContingencyList idBasedContingencyList) {
        super();
        init(idBasedContingencyList.getIdentifierContingencyList());
    }

    final void init(IdentifierContingencyList identifierContingencyList) {
        this.identifiersListEntities = new ArrayList<>();
        identifierContingencyList.getIdentifiants().forEach(networkElementIdentifier -> {
            List<NetworkElementIdentifier> identifierList = ((NetworkElementIdentifierList) networkElementIdentifier).getNetworkElementIdentifiers();
            String contingencyName = networkElementIdentifier.getContingencyId().isPresent() ? networkElementIdentifier.getContingencyId().get() : "";
            identifiersListEntities.add(new IdentifierListEntity(UUID.randomUUID(), contingencyName, identifierList.stream().map(identifier -> ((IdBasedNetworkElementIdentifier) identifier).getIdentifier()).collect(Collectors.toSet())));
            }
        );
    }

    public IdBasedContingencyListEntity update(IdBasedContingencyList idBasedContingencyList) {
        init(idBasedContingencyList.getIdentifierContingencyList());
        return this;
    }
}

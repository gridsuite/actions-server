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

import javax.persistence.*;
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

    @Column(name = "name")
    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    private List<IdentifierListEntity> identifiersListEntities = new ArrayList<>();

    public IdBasedContingencyListEntity(IdentifierContingencyList identifierContingencyList) {
        super();
        init(identifierContingencyList);
    }

    final void init(IdentifierContingencyList identifierContingencyList) {
        this.name = identifierContingencyList.getName();
        identifierContingencyList.getIdentifiants().forEach(networkElementIdentifier -> {
            List<NetworkElementIdentifier> identifierList = ((NetworkElementIdentifierList) networkElementIdentifier).getIdentifiers();
            String firstIdentifier = ((IdBasedNetworkElementIdentifier) identifierList.stream().findFirst().get()).getIdentifier();
            //TODO: set NetworkIdentifierList name when it will be available in powsybl-core
            identifiersListEntities.add(new IdentifierListEntity(UUID.randomUUID(), firstIdentifier, identifierList.stream().map(identifier -> ((IdBasedNetworkElementIdentifier) identifier).getIdentifier()).collect(Collectors.toSet())));
            }
        );
    }
}

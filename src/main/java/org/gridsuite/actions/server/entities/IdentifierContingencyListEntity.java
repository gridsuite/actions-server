package org.gridsuite.actions.server.entities;

import com.powsybl.contingency.contingency.list.identifier.IdBasedNetworkElementIdentifier;
import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifierList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "identifier_contingency_list")
public class IdentifierContingencyListEntity extends AbstractContingencyEntity {

    @Column(name = "name")
    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    private List<IdentifierListEntity> identifiersListEntities = new ArrayList<>();

    public IdentifierContingencyListEntity(com.powsybl.contingency.contingency.list.IdentifierContingencyList identifierContingencyList) {
        super();
        init(identifierContingencyList);
    }

    final void init(com.powsybl.contingency.contingency.list.IdentifierContingencyList identifierContingencyList) {
        this.name = identifierContingencyList.getName();
        identifierContingencyList.getIdentifiants().forEach(networkElementIdentifierList ->
            //TODO: set IdentifierListEntity ?
            identifiersListEntities.add(new IdentifierListEntity(UUID.randomUUID(), UUID.randomUUID().toString(), ((NetworkElementIdentifierList) networkElementIdentifierList).getIdentifiers().stream().map(identifier -> ((IdBasedNetworkElementIdentifier) identifier).getIdentifier()).collect(Collectors.toSet())))
        );
    }
}

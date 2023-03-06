package org.gridsuite.actions.server.entities;

import com.powsybl.contingency.contingency.list.identifier.IdBasedNetworkElementIdentifier;
import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifierList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.actions.server.dto.IdentifierContingencyList;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "identifier_contingency_list")
public class IdentifierContingencyListEntity extends AbstractContingencyEntity {
    @OneToMany(cascade = CascadeType.ALL)
    private List<IdentifierListEntity> identifiersListEntities;

    public IdentifierContingencyListEntity(IdentifierContingencyList identifierContingencyList) {
        super();
        init(identifierContingencyList);
    }

    final void init(IdentifierContingencyList identifierContingencyList) {
        identifierContingencyList.getListOfNetworkElementIdentifierList().forEach(networkElementIdentifierList -> {
            identifiersListEntities.add(new IdentifierListEntity(null, networkElementIdentifierList.getIdentifiers().stream().map(identifier -> ((IdBasedNetworkElementIdentifier) identifier).getIdentifier()).collect(Collectors.toSet())));
        });
    }
}

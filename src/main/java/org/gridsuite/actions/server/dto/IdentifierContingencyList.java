package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifierList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Identifiers contingency list")
public class IdentifierContingencyList implements ContingencyList {
    @Schema(description = "Contingency list id")
    private UUID id;

    @Schema(description = "list id")
    private List<NetworkElementIdentifierList> listOfNetworkElementIdentifierList;

    @Schema(description = "Type")
    @Override
    public ContingencyListType getType() {
        return ContingencyListType.IDENTIFIERS;
    }
}

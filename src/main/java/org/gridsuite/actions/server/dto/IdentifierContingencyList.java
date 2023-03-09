package org.gridsuite.actions.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Identifier contingency list")
public class IdentifierContingencyList implements ContingencyList {

    @Schema(description = "List id")
    private UUID id;

    @Schema(description = "Identifier list")
    private com.powsybl.contingency.contingency.list.IdentifierContingencyList identifierContingencyList;

    @Schema(description = "Type")
    @Override
    public ContingencyListType getType() {
        return ContingencyListType.IDENTIFIERS;
    }
}

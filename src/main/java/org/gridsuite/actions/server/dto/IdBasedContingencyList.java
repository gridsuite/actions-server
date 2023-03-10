package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Id based contingency list")
public class IdBasedContingencyList implements ContingencyList {

    @Schema(description = "List id")
    private UUID id;

    @Schema(description = "Identifier list")
    private IdentifierContingencyList identifierContingencyList;

    @Schema(description = "Type")
    @Override
    public ContingencyListType getType() {
        return ContingencyListType.IDENTIFIERS;
    }
}

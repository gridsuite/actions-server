package org.gridsuite.actions.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Metadata on filters composing filter based contingency list")
public class FilterMetaData {
    @Schema(description = "filter uuid in filter data base")
    private UUID id;

    @Schema(description = "filter name")
    private String name;

    @Schema(description = "equipment type")
    private String equipmentType;
}

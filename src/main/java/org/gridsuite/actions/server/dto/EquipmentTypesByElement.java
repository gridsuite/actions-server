package org.gridsuite.actions.server.dto;

import com.powsybl.iidm.network.IdentifiableType;
import org.gridsuite.actions.server.entities.EquipmentTypesByElementEntity;

import java.util.Set;
import java.util.UUID;

/**
 * Store a list of equipment types for a given element ID
 *
 * @author Florent MILLOT <florent.millot@rte-france.com>
 */
public record EquipmentTypesByElement(UUID id, Set<IdentifiableType> equipmentTypes) {
    public EquipmentTypesByElementEntity toEntity() {
        return new EquipmentTypesByElementEntity(id, equipmentTypes);
    }
}

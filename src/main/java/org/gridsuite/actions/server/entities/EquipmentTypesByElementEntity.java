package org.gridsuite.actions.server.entities;

import com.powsybl.iidm.network.IdentifiableType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.dto.EquipmentTypesByElement;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Set;
import java.util.UUID;

/**
 * Store a list of equipment types for a given element ID
 *
 * @author Florent MILLOT <florent.millot@rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class EquipmentTypesByElementEntity {

    @Id
    @Column
    @Schema(description = "ID of the element")
    private UUID id;

    @Column
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable
    @Fetch(FetchMode.JOIN)
    @Schema(description = "List of associated equipment types")
    Set<IdentifiableType> equipmentTypes;

    public EquipmentTypesByElement toDto() {
        return new EquipmentTypesByElement(id, equipmentTypes);
    }

}

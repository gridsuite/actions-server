package org.gridsuite.actions.server.entities;

import com.powsybl.iidm.network.IdentifiableType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.dto.EquipmentTypesByFilterId;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Set;
import java.util.UUID;

/**
 * Store a list of equipment types for a given filter ID
 *
 * @author Florent MILLOT <florent.millot@rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "equipment_types_by_filter_id")
public class EquipmentTypesByFilterIdEntity {

    @Id
    @Column(name = "filter_id")
    @Schema(description = "ID of the filter")
    private UUID filterId;

    @Column(name = "equipment_type")
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "equipment_types_by_filter_id_equipment_types",
        joinColumns = @JoinColumn(name = "equipment_types_by_filter_id_filter_id"),
        foreignKey = @ForeignKey(name = "equipment_types_by_filter_id_equipment_types_fk"))
    @Fetch(FetchMode.JOIN)
    @Schema(description = "List of associated equipment types")
    Set<IdentifiableType> equipmentTypes;

    public EquipmentTypesByFilterId toDto() {
        return new EquipmentTypesByFilterId(filterId, equipmentTypes);
    }

}

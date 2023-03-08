package org.gridsuite.actions.server.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
@Entity
@Table(name = "identifier_list")
public class IdentifierListEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "equipmentIds")
    @ElementCollection
    @CollectionTable(foreignKey = @ForeignKey(name = "identifierListEntity_equipmentIds_fk1"), indexes = {@Index(name = "identifierListEntity_equipmentIds_idx1", columnList = "identifier_list_entity_id")})
    Set<String> equipmentIds;
}

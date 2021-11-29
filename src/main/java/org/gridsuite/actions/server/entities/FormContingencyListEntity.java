/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.gridsuite.actions.server.dto.FormContingencyListAttributes;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.collections4.SetUtils.emptyIfNull;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "form_contingency_list")
public class FormContingencyListEntity extends AbstractContingencyEntity {

    @Column(name = "equipmentId")
    private String equipmentId;

    @Column(name = "equipmentName")
    private String equipmentName;

    @Column(name = "equipmentType")
    private String equipmentType;

    @Column(name = "nominalVoltage")
    private double nominalVoltage;

    @Column(name = "nominalVoltageOperator")
    private String nominalVoltageOperator;

    @Column(name = "country")
    @ElementCollection
    @CollectionTable(foreignKey = @ForeignKey(name = "formContingencyListEntity_countries_fk"), indexes = {@Index(name = "formContingencyListEntity_countries_idx", columnList = "form_contingency_list_entity_id")})
    private Set<String> countries;

    public FormContingencyListEntity(FormContingencyListAttributes formContingencyListAttributes) {
        super();
        init(formContingencyListAttributes);
    }

    /* called in constructor so it is final */
    final void init(FormContingencyListAttributes formContingencyListAttributes) {
        this.equipmentId = formContingencyListAttributes.getEquipmentID();
        this.equipmentName = formContingencyListAttributes.getEquipmentName();
        this.equipmentType = formContingencyListAttributes.getEquipmentType();
        this.nominalVoltage = formContingencyListAttributes.getNominalVoltage();
        this.nominalVoltageOperator = formContingencyListAttributes.getNominalVoltageOperator();
        this.countries = new HashSet<>(emptyIfNull(formContingencyListAttributes.getCountries()));
    }

    public FormContingencyListEntity update(FormContingencyListAttributes formContingencyListAttributes) {
        init(formContingencyListAttributes);
        return this;
    }
}

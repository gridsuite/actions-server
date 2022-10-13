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
import org.gridsuite.actions.server.dto.FormContingencyList;

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

    @Column(name = "country2")
    @ElementCollection
    @CollectionTable(foreignKey = @ForeignKey(name = "formContingencyListEntity_countries2_fk"), indexes = {@Index(name = "formContingencyListEntity_countries2_idx", columnList = "form_contingency_list_entity_id")})
    private Set<String> countries2;

    public FormContingencyListEntity(FormContingencyList formContingencyList) {
        super();
        init(formContingencyList);
    }

    /* called in constructor so it is final */
    final void init(FormContingencyList formContingencyList) {
        this.equipmentType = formContingencyList.getEquipmentType();
        this.nominalVoltage = formContingencyList.getNominalVoltage();
        this.nominalVoltageOperator = formContingencyList.getNominalVoltageOperator();
        this.countries = new HashSet<>(emptyIfNull(formContingencyList.getCountries()));
        this.countries2 = new HashSet<>(emptyIfNull(formContingencyList.getCountries2()));
    }

    public FormContingencyListEntity update(FormContingencyList formContingencyList) {
        init(formContingencyList);
        return this;
    }
}

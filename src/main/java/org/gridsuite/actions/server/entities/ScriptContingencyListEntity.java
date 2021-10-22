/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.actions.server.dto.ScriptContingencyList;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "script_contingency_list", indexes = @Index(name = "script_contingency_list_name_idx", columnList = "name"))
public class ScriptContingencyListEntity extends AbstractContingencyEntity {

    @Lob
    @Column(name = "script", columnDefinition = "TEXT")
    private String script;

    public ScriptContingencyListEntity(ScriptContingencyList list) {
        super();
        init(list);
    }

    protected final void init(ScriptContingencyList list) {
        super.init(list);
        this.script = list.getScript();
    }

    public ScriptContingencyListEntity update(ScriptContingencyList list) {
        init(list);
        return this;
    }
}

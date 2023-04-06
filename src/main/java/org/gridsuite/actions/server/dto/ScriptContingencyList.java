/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.contingency.list.ContingencyList;
import com.powsybl.contingency.dsl.GroovyContingencyListLoader;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.UUID;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Getter
@NoArgsConstructor
@Schema(description = "Script contingency list")
public class ScriptContingencyList extends AbstractContingencyList {

    @Schema(description = "Script")
    private String script;

    public ScriptContingencyList(UUID uuid, Date date, String script) {
        super(new ContingencyListMetadataImpl(uuid, ContingencyListType.SCRIPT, date));
        this.script = script;
    }

    @Override
    public ContingencyList toPowsyblContingencyList() {
        ImportCustomizer customizer = new ImportCustomizer();
        customizer.addImports("org.gridsuite.actions.server.utils.FiltersUtils");
        customizer.addStaticStars("org.gridsuite.actions.server.utils.FiltersUtils");
        return new GroovyContingencyListLoader().load(this.getId().toString(), new ByteArrayInputStream(script.getBytes()));
    }
}

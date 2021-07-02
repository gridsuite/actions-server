/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.test;

import org.gridsuite.actions.server.FiltersToGroovyScript;
import org.gridsuite.actions.server.dto.FiltersContingencyListAttributes;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class GenerateScriptFromFiltersTest {
    @Test
    public void generateScriptTest() {
        FiltersToGroovyScript filtersToScript = new FiltersToGroovyScript();

        LinkedHashSet<String> countries = new LinkedHashSet<>();
        countries.add("FR");
        countries.add("BE");

        assertEquals("import org.gridsuite.actions.server.utils.FiltersUtils;\n" +
            "\n" +
            "for (equipment in network.generators) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV == 90.0)\n" +
            "      && (FiltersUtils.matchID('BRESS*', equipment) || FiltersUtils.matchName('OTHER*', equipment))\n" +
            "      && FiltersUtils.isLocatedIn(['FR','BE'], equipment)\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes(null, null, "BRESS*",
            "OTHER*",
            "GENERATOR",
            90,
            "=",
            countries,
            null)));

        assertEquals("import org.gridsuite.actions.server.utils.FiltersUtils;\n" +
            "\n" +
            "for (equipment in network.twoWindingsTransformers) {\n" +
            "  if ((FiltersUtils.matchID('*', equipment) || FiltersUtils.matchName('*', equipment))\n" +
            "     ) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", filtersToScript.generateGroovyScriptFromFilters(
            new FiltersContingencyListAttributes(null, null,
                "*",
                "*",
                "TWO_WINDINGS_TRANSFORMER",
                -1,
                ">=",
                new HashSet<>(),
                null
            )));

        assertEquals("import org.gridsuite.actions.server.utils.FiltersUtils;\n" +
            "\n" +
            "for (equipment in network.hvdcLines) {\n" +
            "  if ((FiltersUtils.matchID('BAIXA*', equipment) || FiltersUtils.matchName('*', equipment))\n" +
            "      && (FiltersUtils.isLocatedIn(['FR','BE'], equipment.converterStation1)\n" +
            "          || FiltersUtils.isLocatedIn(['FR','BE'], equipment.converterStation2))) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes(null, null, "BAIXA*",
            "*",
            "HVDC_LINE",
            -1,
            "<=",
            countries,
            null)));

        assertEquals("import org.gridsuite.actions.server.utils.FiltersUtils;\n" +
            "\n" +
            "for (equipment in network.danglingLines) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV == 225.0)\n" +
            "      && (FiltersUtils.matchID('*', equipment) || FiltersUtils.matchName('*', equipment))\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes(
            null, null,
            "*",
            "*",
            "DANGLING_LINE",
            225,
            "=",
            new HashSet<>(),
            null)));

        assertEquals("import org.gridsuite.actions.server.utils.FiltersUtils;\n" +
            "\n" +
            "for (equipment in network.staticVarCompensators) {\n" +
            "  if ((FiltersUtils.matchID('SVC*', equipment) || FiltersUtils.matchName('*', equipment))\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes(null, null, "SVC*",
            "*",
            "STATIC_VAR_COMPENSATOR",
            -1,
            "=",
            new HashSet<>(), null)));

        assertEquals("import org.gridsuite.actions.server.utils.FiltersUtils;\n" +
            "\n" +
            "for (equipment in network.shuntCompensators) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV < 90.0)\n" +
            "      && (FiltersUtils.matchID('*', equipment) || FiltersUtils.matchName('SHUNT*', equipment))\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes(null, null, "*",
            "SHUNT*",
            "SHUNT_COMPENSATOR",
            90,
            "<",
            new HashSet<>(), null)));

        assertEquals("import org.gridsuite.actions.server.utils.FiltersUtils;\n" +
            "\n" +
            "for (equipment in network.lines) {\n" +
            "  if ((equipment.terminal1.voltageLevel.nominalV == 225.0\n" +
            "          || equipment.terminal2.voltageLevel.nominalV == 225.0)\n" +
            "      && (FiltersUtils.matchID('*', equipment) || FiltersUtils.matchName('*', equipment))\n" +
            "     ) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes(null, null, "*",
            "*",
            "LINE",
            225,
            "=",
            new HashSet<>(), null)));

        assertEquals("import org.gridsuite.actions.server.utils.FiltersUtils;\n" +
            "\n" +
            "for (equipment in network.busbarSections) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV >= 63.0)\n" +
            "      && (FiltersUtils.matchID('BBS*', equipment) || FiltersUtils.matchName('BBS*', equipment))\n" +
            "      && FiltersUtils.isLocatedIn(['FR','BE'], equipment)\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes(null, null, "BBS*",
            "BBS*",
            "BUSBAR_SECTION",
            63,
            ">=",
            countries, null)));
    }
}

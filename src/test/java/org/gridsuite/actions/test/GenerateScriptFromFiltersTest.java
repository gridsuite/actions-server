/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.test;

import org.gridsuite.actions.server.FormToGroovyScript;
import org.gridsuite.actions.server.dto.FormContingencyListAttributes;
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
        FormToGroovyScript formToScript = new FormToGroovyScript();

        LinkedHashSet<String> countries = new LinkedHashSet<>();
        countries.add("FR");
        countries.add("BE");

        assertEquals("for (equipment in network.generators) {\n" +
                "  if ((equipment.terminal.voltageLevel.nominalV == 90.0)\n" +
                "      && (matchID('BRESS*', equipment) || matchName('OTHER*', equipment))\n" +
                "      && isLocatedIn(['FR','BE'], equipment)\n" +
                "     ) {\n" +
                "        contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyListAttributes(null, "BRESS*",
            "OTHER*",
            "GENERATOR",
            90,
            "=",
            countries)));

        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
            "  if ((matchID('*', equipment) || matchName('*', equipment))\n" +
            "     ) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(
            new FormContingencyListAttributes(null,
                "*",
                "*",
                "TWO_WINDINGS_TRANSFORMER",
                -1,
                ">=",
                new HashSet<>()
            )));

        assertEquals("for (equipment in network.hvdcLines) {\n" +
            "  if ((matchID('BAIXA*', equipment) || matchName('*', equipment))\n" +
            "      && (isLocatedIn(['FR','BE'], equipment.converterStation1)\n" +
            "          || isLocatedIn(['FR','BE'], equipment.converterStation2))) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyListAttributes(null, "BAIXA*",
            "*",
            "HVDC_LINE",
            -1,
            "<=",
            countries)));

        assertEquals("for (equipment in network.danglingLines) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV == 225.0)\n" +
            "      && (matchID('*', equipment) || matchName('*', equipment))\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyListAttributes(
            null,
            "*",
            "*",
            "DANGLING_LINE",
            225,
            "=",
            new HashSet<>())));

        assertEquals("for (equipment in network.staticVarCompensators) {\n" +
            "  if ((matchID('SVC*', equipment) || matchName('*', equipment))\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyListAttributes(null, "SVC*",
            "*",
            "STATIC_VAR_COMPENSATOR",
            -1,
            "=",
            new HashSet<>())));

        assertEquals("for (equipment in network.shuntCompensators) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV < 90.0)\n" +
            "      && (matchID('*', equipment) || matchName('SHUNT*', equipment))\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyListAttributes(null, "*",
            "SHUNT*",
            "SHUNT_COMPENSATOR",
            90,
            "<",
            new HashSet<>())));

        assertEquals("for (equipment in network.lines) {\n" +
            "  if ((equipment.terminal1.voltageLevel.nominalV == 225.0\n" +
            "          || equipment.terminal2.voltageLevel.nominalV == 225.0)\n" +
            "      && (matchID('*', equipment) || matchName('*', equipment))\n" +
            "     ) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyListAttributes(null, "*",
            "*",
            "LINE",
            225,
            "=",
            new HashSet<>())));

        assertEquals("for (equipment in network.busbarSections) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV >= 63.0)\n" +
            "      && (matchID('BBS*', equipment) || matchName('BBS*', equipment))\n" +
            "      && isLocatedIn(['FR','BE'], equipment)\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyListAttributes(null, "BBS*",
            "BBS*",
            "BUSBAR_SECTION",
            63,
            ">=",
            countries)));
    }
}

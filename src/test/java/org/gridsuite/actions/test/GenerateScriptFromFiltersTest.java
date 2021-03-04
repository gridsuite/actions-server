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

        assertEquals("import org.springframework.util.AntPathMatcher\n" +
                "import org.springframework.util.PathMatcher\n" +
                "\n" +
                "PathMatcher antMatcher = new AntPathMatcher(\"\\0\")\n" +
                "\n" +
                "noNominalV = 90.0 == -1\n" +
                "noCountries = ['FR','BE'].isEmpty()\n" +
                "\n" +
                "for (equipment in network.generators) {\n" +
                "  if (   (noNominalV || equipment.terminal.voltageLevel.nominalV == 90.0)\n" +
                "         && (antMatcher.match('BRESS*', equipment.id) || (equipment.optionalName.isPresent() && antMatcher.match('OTHER*', equipment.name)))\n" +
                "         && (noCountries || equipment.terminals.stream().anyMatch{connectable ->\n" +
                "             connectable.voltageLevel.substation.country.isPresent() && ['FR','BE'].contains(connectable.voltageLevel.substation.country.get().name())})) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "         }\n" +
                "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes("BRESS*",
                "OTHER*",
                "GENERATOR",
                90,
                "=",
                countries)));

        assertEquals("import org.springframework.util.AntPathMatcher\n" +
                "import org.springframework.util.PathMatcher\n" +
                "\n" +
                "PathMatcher antMatcher = new AntPathMatcher(\"\\0\")\n" +
                "\n" +
                "noNominalV = -1.0 == -1\n" +
                "noCountries = [].isEmpty()\n" +
                "\n" +
                "for (equipment in network.twoWindingsTransformers) {\n" +
                "  if (   (noNominalV || equipment.terminal1.voltageLevel.nominalV >= -1.0\n" +
                "          || equipment.terminal2.voltageLevel.nominalV >= -1.0)\n" +
                "         && (antMatcher.match('*', equipment.id) || (equipment.optionalName.isPresent() && antMatcher.match('*', equipment.name)))\n" +
                "         && (noCountries || equipment.terminals.stream().anyMatch{connectable ->\n" +
                "             connectable.voltageLevel.substation.country.isPresent() && [].contains(connectable.voltageLevel.substation.country.get().name())})) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "         }\n" +
                "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes("*",
                "*",
                "TWO_WINDINGS_TRANSFORMER",
                -1,
                ">=",
                new HashSet<>())));

        assertEquals("import org.springframework.util.AntPathMatcher\n" +
                "import org.springframework.util.PathMatcher\n" +
                "\n" +
                "PathMatcher antMatcher = new AntPathMatcher(\"\\0\")\n" +
                "\n" +
                "noNominalV = -1.0 == -1\n" +
                "noCountries = ['FR','BE'].isEmpty()\n" +
                "\n" +
                "for (equipment in network.hvdcLines) {\n" +
                "  if (   (noNominalV || equipment.nominalV <= -1.0)\n" +
                "         && (antMatcher.match('BAIXA*', equipment.id) || (equipment.optionalName.isPresent() && antMatcher.match('*', equipment.name)))\n" +
                "         && (noCountries ||\n" +
                "             equipment.converterStation1.terminals.stream().anyMatch{connectable ->\n" +
                "                     connectable.voltageLevel.substation.country.isPresent() && ['FR','BE'].contains(connectable.voltageLevel.substation.country.get().name())} ||\n" +
                "             equipment.converterStation2.terminals.stream().anyMatch{connectable ->\n" +
                "                     connectable.voltageLevel.substation.country.isPresent() && ['FR','BE'].contains(connectable.voltageLevel.substation.country.get().name())})) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "         }\n" +
                "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes("BAIXA*",
                "*",
                "HVDC_LINE",
                -1,
                "<=",
                countries)));

        assertEquals("import org.springframework.util.AntPathMatcher\n" +
                "import org.springframework.util.PathMatcher\n" +
                "\n" +
                "PathMatcher antMatcher = new AntPathMatcher(\"\\0\")\n" +
                "\n" +
                "noNominalV = 225.0 == -1\n" +
                "noCountries = [].isEmpty()\n" +
                "\n" +
                "for (equipment in network.danglingLines) {\n" +
                "  if (   (noNominalV || equipment.terminal.voltageLevel.nominalV == 225.0)\n" +
                "         && (antMatcher.match('*', equipment.id) || (equipment.optionalName.isPresent() && antMatcher.match('*', equipment.name)))\n" +
                "         && (noCountries || equipment.terminals.stream().anyMatch{connectable ->\n" +
                "             connectable.voltageLevel.substation.country.isPresent() && [].contains(connectable.voltageLevel.substation.country.get().name())})) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "         }\n" +
                "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes("*",
                "*",
                "DANGLING_LINE",
                225,
                "=",
                new HashSet<>())));

        assertEquals("import org.springframework.util.AntPathMatcher\n" +
                "import org.springframework.util.PathMatcher\n" +
                "\n" +
                "PathMatcher antMatcher = new AntPathMatcher(\"\\0\")\n" +
                "\n" +
                "noNominalV = -1.0 == -1\n" +
                "noCountries = [].isEmpty()\n" +
                "\n" +
                "for (equipment in network.staticVarCompensators) {\n" +
                "  if (   (noNominalV || equipment.terminal.voltageLevel.nominalV == -1.0)\n" +
                "         && (antMatcher.match('SVC*', equipment.id) || (equipment.optionalName.isPresent() && antMatcher.match('*', equipment.name)))\n" +
                "         && (noCountries || equipment.terminals.stream().anyMatch{connectable ->\n" +
                "             connectable.voltageLevel.substation.country.isPresent() && [].contains(connectable.voltageLevel.substation.country.get().name())})) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "         }\n" +
                "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes("SVC*",
                "*",
                "STATIC_VAR_COMPENSATOR",
                -1,
                "=",
                new HashSet<>())));

        assertEquals("import org.springframework.util.AntPathMatcher\n" +
                "import org.springframework.util.PathMatcher\n" +
                "\n" +
                "PathMatcher antMatcher = new AntPathMatcher(\"\\0\")\n" +
                "\n" +
                "noNominalV = 90.0 == -1\n" +
                "noCountries = [].isEmpty()\n" +
                "\n" +
                "for (equipment in network.shuntCompensators) {\n" +
                "  if (   (noNominalV || equipment.terminal.voltageLevel.nominalV < 90.0)\n" +
                "         && (antMatcher.match('*', equipment.id) || (equipment.optionalName.isPresent() && antMatcher.match('SHUNT*', equipment.name)))\n" +
                "         && (noCountries || equipment.terminals.stream().anyMatch{connectable ->\n" +
                "             connectable.voltageLevel.substation.country.isPresent() && [].contains(connectable.voltageLevel.substation.country.get().name())})) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "         }\n" +
                "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes("*",
                "SHUNT*",
                "SHUNT_COMPENSATOR",
                90,
                "<",
                new HashSet<>())));

        assertEquals("import org.springframework.util.AntPathMatcher\n" +
                "import org.springframework.util.PathMatcher\n" +
                "\n" +
                "PathMatcher antMatcher = new AntPathMatcher(\"\\0\")\n" +
                "\n" +
                "noNominalV = 225.0 == -1\n" +
                "noCountries = [].isEmpty()\n" +
                "\n" +
                "for (equipment in network.lines) {\n" +
                "  if (   (noNominalV || equipment.terminal1.voltageLevel.nominalV == 225.0\n" +
                "          || equipment.terminal2.voltageLevel.nominalV == 225.0)\n" +
                "         && (antMatcher.match('*', equipment.id) || (equipment.optionalName.isPresent() && antMatcher.match('*', equipment.name)))\n" +
                "         && (noCountries || equipment.terminals.stream().anyMatch{connectable ->\n" +
                "             connectable.voltageLevel.substation.country.isPresent() && [].contains(connectable.voltageLevel.substation.country.get().name())})) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "         }\n" +
                "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes("*",
                "*",
                "LINE",
                225,
                "=",
                new HashSet<>())));

        assertEquals("import org.springframework.util.AntPathMatcher\n" +
                "import org.springframework.util.PathMatcher\n" +
                "\n" +
                "PathMatcher antMatcher = new AntPathMatcher(\"\\0\")\n" +
                "\n" +
                "noNominalV = 63.0 == -1\n" +
                "noCountries = ['FR','BE'].isEmpty()\n" +
                "\n" +
                "for (equipment in network.busbarSections) {\n" +
                "  if (   (noNominalV || equipment.terminal.voltageLevel.nominalV >= 63.0)\n" +
                "         && (antMatcher.match('BBS*', equipment.id) || (equipment.optionalName.isPresent() && antMatcher.match('BBS*', equipment.name)))\n" +
                "         && (noCountries || equipment.terminals.stream().anyMatch{connectable ->\n" +
                "             connectable.voltageLevel.substation.country.isPresent() && ['FR','BE'].contains(connectable.voltageLevel.substation.country.get().name())})) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "         }\n" +
                "}\n", filtersToScript.generateGroovyScriptFromFilters(new FiltersContingencyListAttributes("BBS*",
                "BBS*",
                "BUSBAR_SECTION",
                63,
                ">=",
                countries)));
    }
}

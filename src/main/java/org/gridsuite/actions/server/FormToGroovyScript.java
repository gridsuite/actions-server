/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.commons.PowsyblException;
import org.apache.commons.io.IOUtils;
import org.gridsuite.actions.server.dto.FormContingencyList;
import org.gridsuite.actions.server.utils.EquipmentType;
import org.gridsuite.actions.server.utils.NumericalFilterOperator;
import org.springframework.core.io.ClassPathResource;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.nio.charset.Charset;

import static java.util.stream.Collectors.joining;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class FormToGroovyScript {
    private final String branchTemplate;
    private final String injectionTemplate;
    private final String hvdcLineTemplate;

    public FormToGroovyScript() {
        try {
            branchTemplate = IOUtils.toString(new ClassPathResource("branch.st").getInputStream(), Charset.defaultCharset());
            injectionTemplate = IOUtils.toString(new ClassPathResource("injection.st").getInputStream(), Charset.defaultCharset());
            hvdcLineTemplate = IOUtils.toString(new ClassPathResource("hvdcLine.st").getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new PowsyblException("Unable to load templates for groovy script generation !!");
        }
    }

    public String generateGroovyScriptFromForm(FormContingencyList formContingencyList) {
        String script = "";
        String equipmentsCollection = "";

        switch (EquipmentType.valueOf(formContingencyList.getEquipmentType())) {
            case GENERATOR:
                equipmentsCollection = "generators";
                script = injectionTemplate;
                break;
            case STATIC_VAR_COMPENSATOR:
                equipmentsCollection = "staticVarCompensators";
                script = injectionTemplate;
                break;
            case SHUNT_COMPENSATOR:
                equipmentsCollection = "shuntCompensators";
                script = injectionTemplate;
                break;
            case BUSBAR_SECTION:
                equipmentsCollection = "busbarSections";
                script = injectionTemplate;
                break;
            case DANGLING_LINE:
                equipmentsCollection = "danglingLines";
                script = injectionTemplate;
                break;
            case HVDC_LINE:
                equipmentsCollection = "hvdcLines";
                script = hvdcLineTemplate;
                break;
            case LINE:
                equipmentsCollection = "lines";
                script = branchTemplate;
                break;
            case TWO_WINDINGS_TRANSFORMER:
                equipmentsCollection = "twoWindingsTransformers";
                script = branchTemplate;
                break;
            default:
                throw new PowsyblException("Unknown equipment type");
        }

        ST template = new ST(script);

        template.add("collectionName", equipmentsCollection);
        if (formContingencyList.getNominalVoltage1() != null) {
            template.add("nominalV", formContingencyList.getNominalVoltage1().getValue1());
            template.add("nominalVOperator", NumericalFilterOperator.toScript(formContingencyList.getNominalVoltage1().getType()));
        }
        // TODO DBR range
        if (!formContingencyList.getCountries().isEmpty()) {
            template.add("countries", formContingencyList.getCountries().stream().collect(joining("','", "['", "']")));
        }

        return template.render();
    }
}

package org.gridsuite.actions.server.utils;

import com.powsybl.contingency.BatteryContingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.DanglingLineContingency;
import com.powsybl.contingency.GeneratorContingency;
import com.powsybl.contingency.HvdcLineContingency;
import com.powsybl.contingency.LineContingency;
import com.powsybl.contingency.LoadContingency;
import com.powsybl.contingency.ShuntCompensatorContingency;
import com.powsybl.contingency.StaticVarCompensatorContingency;
import com.powsybl.contingency.ThreeWindingsTransformerContingency;
import com.powsybl.contingency.TwoWindingsTransformerContingency;
import org.gridsuite.filter.identifierlistfilter.IdentifiableAttributes;

/**
 * @author Bassel El Cheikh <bassel.el-cheikh at rte-france.com>
 */

public final class ContingencyListUtils {

    private ContingencyListUtils() {
        // Utility class no constructor
    }

    public static ContingencyElement toContingencyElement(IdentifiableAttributes id) {
        switch (id.getType()) {
            case LINE -> {
                return new LineContingency(id.getId());
            }
            case TWO_WINDINGS_TRANSFORMER -> {
                return new TwoWindingsTransformerContingency(id.getId());
            }
            case THREE_WINDINGS_TRANSFORMER -> {
                return new ThreeWindingsTransformerContingency(id.getId());
            }
            case GENERATOR -> {
                return new GeneratorContingency(id.getId());
            }
            case BATTERY -> {
                return new BatteryContingency(id.getId());
            }
            case LOAD -> {
                return new LoadContingency(id.getId());
            }
            case SHUNT_COMPENSATOR -> {
                return new ShuntCompensatorContingency(id.getId());
            }
            case STATIC_VAR_COMPENSATOR -> {
                return new StaticVarCompensatorContingency(id.getId());
            }
            case HVDC_LINE -> {
                return new HvdcLineContingency(id.getId());
            }
            case DANGLING_LINE -> {
                return new DanglingLineContingency(id.getId());
            }
            default -> throw new IllegalStateException("Unexpected value: " + id.getType());
        }
    }
}

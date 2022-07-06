/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.utils;

import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Substation;

import java.util.List;
import java.util.Optional;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public final class FiltersUtils {
    private FiltersUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static boolean isLocatedIn(List<String> filterCountries, Connectable<?> equipment) {
        return filterCountries.isEmpty() || equipment.getTerminals().stream().anyMatch(t -> {
            Optional<Country> country = t.getVoltageLevel().getSubstation().flatMap(Substation::getCountry);
            return country.map(c -> filterCountries.contains(c.name())).orElse(false);
        });
    }
}

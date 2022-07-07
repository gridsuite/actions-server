/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.test;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import org.gridsuite.actions.server.utils.FiltersUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class FiltersUtilsTest {
    @Test
    public void test() {
        Network network = EurostagTutorialExample1Factory.createWithMoreGenerators(new NetworkFactoryImpl());
        Connectable<Generator> generator = network.getGenerator("GEN");

        assertNotNull(generator);
        assertTrue(FiltersUtils.isLocatedIn(List.of("FR", "BE"), generator));
        assertFalse(FiltersUtils.isLocatedIn(List.of("DE"), generator));
    }
}

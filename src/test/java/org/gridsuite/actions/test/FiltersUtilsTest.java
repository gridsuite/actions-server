/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.test;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import org.gridsuite.actions.server.utils.FiltersUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class FiltersUtilsTest {
    @Test
    public void test() {
        Identifiable<Generator> ident = Mockito.mock(Identifiable.class);
        Mockito.when(ident.getId()).thenReturn("id1");
        Mockito.when(ident.getOptionalName()).thenReturn(Optional.of("name1"));

        assertTrue(FiltersUtils.matchID("id1", ident));
        assertFalse(FiltersUtils.matchID("id2", ident));

        assertTrue(FiltersUtils.matchName("name1", ident));
        assertFalse(FiltersUtils.matchName("name2", ident));
    }
}

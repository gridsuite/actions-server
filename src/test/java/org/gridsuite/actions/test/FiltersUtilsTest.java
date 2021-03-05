/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.test;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import org.gridsuite.actions.server.utils.FiltersUtils;
import org.junit.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class FiltersUtilsTest {
    @Test
    public void test() {
        Identifiable<Generator> ident = new Identifiable<Generator>() {
            @Override
            public Network getNetwork() {
                return null;
            }

            @Override
            public String getId() {
                return "id1";
            }

            @Override
            public Optional<String> getOptionalName() {
                return Optional.of("name1");
            }

            @Override
            public boolean hasProperty() {
                return false;
            }

            @Override
            public boolean hasProperty(String s) {
                return false;
            }

            @Override
            public String getProperty(String s) {
                return null;
            }

            @Override
            public String getProperty(String s, String s1) {
                return null;
            }

            @Override
            public String setProperty(String s, String s1) {
                return null;
            }

            @Override
            public Set<String> getPropertyNames() {
                return null;
            }

            @Override
            public <E extends Extension<Generator>> void addExtension(Class<? super E> aClass, E e) {
            }

            @Override
            public <E extends Extension<Generator>> E getExtension(Class<? super E> aClass) {
                return null;
            }

            @Override
            public <E extends Extension<Generator>> E getExtensionByName(String s) {
                return null;
            }

            @Override
            public <E extends Extension<Generator>> boolean removeExtension(Class<E> aClass) {
                return false;
            }

            @Override
            public <E extends Extension<Generator>> Collection<E> getExtensions() {
                return null;
            }
        };

        assertTrue(FiltersUtils.matchID("id1", ident));
        assertFalse(FiltersUtils.matchID("id2", ident));

        assertTrue(FiltersUtils.matchName("name1", ident));
        assertFalse(FiltersUtils.matchName("name2", ident));
    }
}

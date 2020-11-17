/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

/**
 * @author Jon Harper <jon.harper at rte-france.com>
 */
public class RenameContingencyListAttributes {
    private String newContingencyListName;

    public String getNewContingencyListName() {
        return newContingencyListName;
    }

    public void setNewContingencyListName(String newContingencyListName) {
        this.newContingencyListName = newContingencyListName;
    }

    public RenameContingencyListAttributes() {
    }

    public RenameContingencyListAttributes(String newContingencyListName) {
        setNewContingencyListName(newContingencyListName);
    }
}

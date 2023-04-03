package org.gridsuite.actions.server.dto;

import org.gridsuite.actions.server.utils.ContingencyListType;

import java.util.Date;
import java.util.UUID;

public interface ContingencyListMetadata {

    UUID getId();

    Date getModificationDate();

    ContingencyListType getType();
}

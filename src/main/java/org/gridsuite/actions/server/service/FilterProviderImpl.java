package org.gridsuite.actions.server.service;

import org.gridsuite.actions.FilterProvider;
import org.gridsuite.filter.AbstractFilter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FilterProviderImpl implements FilterProvider {

    private final FilterService filterService;

    public FilterProviderImpl(FilterService filterService) {
        this.filterService = filterService;
    }

    @Override
    public List<AbstractFilter> getFilters(List<UUID> filtersUuids) {
        return filterService.getFilters(filtersUuids);
    }
}

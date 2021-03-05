package org.gridsuite.actions.server.utils;

import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Identifiable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.List;
import java.util.Optional;

public final class FiltersUtils {
    private static final PathMatcher ANT_MATCHER = new AntPathMatcher("\0");

    private FiltersUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static boolean matchID(String filterID, Identifiable<?> equipment) {
        return ANT_MATCHER.match(filterID, equipment.getId());
    }

    public static boolean matchName(String filterName, Identifiable<?> equipment) {
        Optional<String> name = equipment.getOptionalName();
        if (name.isPresent()) {
            return ANT_MATCHER.match(filterName, name.get());
        } else {
            return false;
        }
    }

    public static boolean isLocatedIn(List<String> filterCountries, Connectable<?> equipment) {
        return filterCountries.isEmpty() || equipment.getTerminals().stream().anyMatch(connectable ->
                connectable.getVoltageLevel().getSubstation().getCountry().isPresent()
                        && filterCountries.contains(connectable.getVoltageLevel().getSubstation().getCountry().get().name()));
    }
}

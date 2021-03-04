import org.springframework.util.AntPathMatcher
import org.springframework.util.PathMatcher

PathMatcher antMatcher = new AntPathMatcher("\0")

noNominalV = <nominalV> == -1
noCountries = <countries>.isEmpty()

for (equipment in network.<collectionName>) {
  if (   (noNominalV || equipment.nominalV <nominalVOperator> <nominalV>)
         && (antMatcher.match('<equipmentId>', equipment.id) || (equipment.optionalName.isPresent() && antMatcher.match('<equipmentName>', equipment.name)))
         && (noCountries ||
             equipment.converterStation1.terminals.stream().anyMatch{connectable ->
                     connectable.voltageLevel.substation.country.isPresent() && <countries>.contains(connectable.voltageLevel.substation.country.get().name())} ||
             equipment.converterStation2.terminals.stream().anyMatch{connectable ->
                     connectable.voltageLevel.substation.country.isPresent() && <countries>.contains(connectable.voltageLevel.substation.country.get().name())})) {
           contingency(equipment.id) { equipments equipment.id }
         }
}

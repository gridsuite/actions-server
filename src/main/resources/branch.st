import org.springframework.util.AntPathMatcher
import org.springframework.util.PathMatcher

PathMatcher antMatcher = new AntPathMatcher("\0")

noNominalV = <nominalV> == -1
noCountries = <countries>.isEmpty()

for (equipment in network.<collectionName>) {
  if (   (noNominalV || equipment.terminal1.voltageLevel.nominalV <nominalVOperator> <nominalV>
          || equipment.terminal2.voltageLevel.nominalV <nominalVOperator> <nominalV>)
         && (antMatcher.match('<equipmentId>', equipment.id) || (equipment.optionalName.isPresent() && antMatcher.match('<equipmentName>', equipment.name)))
         && (noCountries || equipment.terminals.stream().anyMatch{connectable ->
             connectable.voltageLevel.substation.country.isPresent() && <countries>.contains(connectable.voltageLevel.substation.country.get().name())})) {
           contingency(equipment.id) { equipments equipment.id }
         }
}

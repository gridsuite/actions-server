import org.springframework.util.AntPathMatcher
import org.springframework.util.PathMatcher

PathMatcher antMatcher = new AntPathMatcher("\0")

for (equipment in network.<collectionName>) {
  if (   (<nominalV> == -1 || equipment.nominalV <nominalVOperator> <nominalV>)
         && (antMatcher.match('<equipmentId>', equipment.id) || equipment.optionalName.isPresent() && antMatcher.match('<equipmentName>', equipment.name))
         && (<countries>.isEmpty() ||
             equipment.converterStation1.terminals.stream().anyMatch{connectable ->
                     connectable.voltageLevel.substation.country.isPresent() && <countries>.contains(connectable.voltageLevel.substation.country.get().name)} ||
             equipment.converterStation2.terminals.stream().anyMatch{connectable ->
                     connectable.voltageLevel.substation.country.isPresent() && <countries>.contains(connectable.voltageLevel.substation.country.get().name)})) {
           contingency(equipment.id) { equipments equipment.id }
         }
}

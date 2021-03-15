import org.gridsuite.actions.server.utils.FiltersUtils;

for (equipment in network.<collectionName>) {
  if (<if(nominalV)>(equipment.terminal1.voltageLevel.nominalV <nominalVOperator> <nominalV>
          || equipment.terminal2.voltageLevel.nominalV <nominalVOperator> <nominalV>)
      && <endif>
      (FiltersUtils.matchID('<equipmentId>', equipment) || FiltersUtils.matchName('<equipmentName>', equipment))
      <if(countries)>&& FiltersUtils.isLocatedIn(<countries>, equipment)<endif>
     ) {
           contingency(equipment.id) { equipments equipment.id }
  }
}

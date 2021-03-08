import org.gridsuite.actions.server.utils.FiltersUtils;

for (equipment in network.<collectionName>) {
  if (   (<nominalV> == -1 || equipment.nominalV <nominalVOperator> <nominalV>)
      && (FiltersUtils.matchID('<equipmentId>', equipment) || FiltersUtils.matchName('<equipmentName>', equipment))
      && (   FiltersUtils.isLocatedIn(<countries>, equipment.converterStation1)
          || FiltersUtils.isLocatedIn(<countries>, equipment.converterStation2))) {
           contingency(equipment.id) { equipments equipment.id }
      }
}

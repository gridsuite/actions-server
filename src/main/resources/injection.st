import org.gridsuite.actions.server.utils.FiltersUtils;

for (equipment in network.<collectionName>) {
  if (   (<nominalV> == -1 || equipment.terminal.voltageLevel.nominalV <nominalVOperator> <nominalV>)
      && (FiltersUtils.matchID('<equipmentId>', equipment) || FiltersUtils.matchName('<equipmentName>', equipment))
      && FiltersUtils.isLocatedIn(<countries>, equipment)) {
        contingency(equipment.id) { equipments equipment.id }
      }
}

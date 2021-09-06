for (equipment in network.<collectionName>) {
  if (<if(nominalV)>(equipment.terminal1.voltageLevel.nominalV <nominalVOperator> <nominalV>
          || equipment.terminal2.voltageLevel.nominalV <nominalVOperator> <nominalV>)
      && <endif>
      (matchID('<equipmentId>', equipment) || matchName('<equipmentName>', equipment))
      <if(countries)>&& isLocatedIn(<countries>, equipment)<endif>
     ) {
           contingency(equipment.id) { equipments equipment.id }
  }
}

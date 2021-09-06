for (equipment in network.<collectionName>) {
  if (<if(nominalV)>(equipment.terminal.voltageLevel.nominalV <nominalVOperator> <nominalV>)
      && <endif>
      (matchID('<equipmentId>', equipment) || matchName('<equipmentName>', equipment))
      <if(countries)>&& isLocatedIn(<countries>, equipment)<endif>
     ) {
        contingency(equipment.id) { equipments equipment.id }
  }
}

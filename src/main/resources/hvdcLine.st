for (equipment in network.<collectionName>) {
  if (<if(nominalV)>(equipment.nominalV <nominalVOperator> <nominalV>)
      && <endif>
      (matchID('<equipmentId>', equipment) || matchName('<equipmentName>', equipment))
      <if(countries)>&& (isLocatedIn(<countries>, equipment.converterStation1)
    || isLocatedIn(<countries>, equipment.converterStation2))<endif>
     ) {
           contingency(equipment.id) { equipments equipment.id }
  }
}

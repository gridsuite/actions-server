for (equipment in network.<collectionName>) {
  if (<if(nominalV)>(equipment.nominalV <nominalVOperator> <nominalV>)
      && <endif>
      (FiltersUtils.matchID('<equipmentId>', equipment) || FiltersUtils.matchName('<equipmentName>', equipment))
      <if(countries)>&& (FiltersUtils.isLocatedIn(<countries>, equipment.converterStation1)
    || FiltersUtils.isLocatedIn(<countries>, equipment.converterStation2))<endif>
     ) {
           contingency(equipment.id) { equipments equipment.id }
  }
}

for (equipment in network.<collectionName>) {
  <if(nominalV || countries)>if (<if(nominalV)>(equipment.terminal.voltageLevel.nominalV <nominalVOperator> <nominalV>)<endif>
    <if(nominalV && countries)>&& <endif><if(countries)>isLocatedIn(<countries>, equipment)<endif>
   ) {
   <endif>
        contingency(equipment.id) { equipments equipment.id }
   <if(nominalV || countries)>
  }
  <endif>
}

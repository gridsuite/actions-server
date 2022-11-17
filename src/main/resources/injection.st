for (equipment in network.<collectionName>) {
  <if(nominalV || nominalVMin || countries1)>if (<if(nominalV)>(equipment.terminal.voltageLevel.nominalV <nominalVOperator> <nominalV>)<endif>
    <if(nominalVMin)>(equipment.terminal.voltageLevel.nominalV >= <nominalVMin> && equipment.terminal.voltageLevel.nominalV \<= <nominalVMax>)<endif>
    <if((nominalV || nominalVMin) && countries1)>&& <endif><if(countries1)>injectionMatch(equipment.terminal, <countries1>)<endif>
   ) {
   <endif>
        contingency(equipment.id) { equipments equipment.id }
   <if(nominalV || nominalVMin || countries1)>
  }
  <endif>
}

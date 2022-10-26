for (equipment in network.<collectionName>) {
  <if(nominalV || nominalVMin || (countries1 && countries2))>if (
      <if(nominalV)>(equipment.terminal1.voltageLevel.nominalV <nominalVOperator> <nominalV> || equipment.terminal2.voltageLevel.nominalV <nominalVOperator> <nominalV>)<endif>
      <if(nominalVMin)>((equipment.terminal1.voltageLevel.nominalV >= <nominalVMin> && equipment.terminal1.voltageLevel.nominalV \<= <nominalVMax>)
      || (equipment.terminal2.voltageLevel.nominalV >= <nominalVMin> && equipment.terminal2.voltageLevel.nominalV \<= <nominalVMax>))<endif>
      <if((nominalV || nominalVMin) && countries1 && countries2)>
      && <endif><if(countries1)>(lineMatch(equipment, <countries1>, <countries2>))<endif>
     ) {
     <endif>
           contingency(equipment.id) { equipments equipment.id }
     <if(nominalV || nominalVMin || (countries1 && countries2))>
  }
   <endif>
}

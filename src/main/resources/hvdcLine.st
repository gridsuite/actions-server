for (equipment in network.<collectionName>) {
  <if(nominalV || nominalVMin || (countries && countries2))>if (<if(nominalV)>(equipment.nominalV <nominalVOperator> <nominalV>)<endif>
      <if(nominalVMin)>(equipment.nominalV >= <nominalVMin> && equipment.nominalV \<= <nominalVMax>)<endif>
      <if(nominalV && countries && countries2)>&& <endif><if(countries)>(hvdcLineMatch(equipment, <countries>, <countries2>))<endif>
     ) {
     <endif>
           contingency(equipment.id) { equipments equipment.id }
     <if(nominalV || countries)>
  }
   <endif>
}

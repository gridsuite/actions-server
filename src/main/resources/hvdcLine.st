for (equipment in network.<collectionName>) {
  <if(nominalV || countries)>if (<if(nominalV)>(equipment.nominalV <nominalVOperator> <nominalV>)<endif>
      <if(nominalV && countries)>&& <endif><if(countries)>(isLocatedIn(<countries>, equipment.converterStation1)
       || isLocatedIn(<countries>, equipment.converterStation2))<endif>
     ) {
     <endif>
           contingency(equipment.id) { equipments equipment.id }
     <if(nominalV || countries)>
  }
   <endif>
}

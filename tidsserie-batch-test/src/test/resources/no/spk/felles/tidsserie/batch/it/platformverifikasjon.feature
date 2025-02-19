# language: no
# encoding: UTF-8

Egenskap: Smoketest av platformrammeverket

  For å sikre at vi fangar opp fatale feil som vil hindre platformrammeverket frå å køyre feilfritt dersom
  modusen ikkje sjølv feilar på noko, har vi satt opp denne egenskapen.

  Vi brukar her ein modus som er heilt tom for funksjonalitet, som kun returnerer "eg er ferdig" utan å gjere noko meir. Dermed får vi flusha ut eventuelle
  fatale feil av typen "batchen kræsjar lenge før den kallar modusen", "batchen kræsjar under opprydding etter modusen har køyrt ferdig osv.

  Merk at smoketesten køyrer batchen to gangar, ein gang in-process for å gjere det enklare å debugge batchkøyringa via
  <INSERT FAVORITE IDE HERE> utan å måtte bygge JAR-fila på nytt etter kvar endring.

  For å vere robuste mot classpathen-problematikk blir batchen og køyrt ein gang out-of-process, då via sist bygde versjon
  av JAR-fila. Får du problem med at batchen ikkje plukkar opp endringar når du køyrer testane på nytt så er det typisk
  fordi du har gløymt å bygge heile tidsserie-batch reaktoren via Maven før du køyrte testen frå din IDE.

  Bakgrunn: Litt semi random grunnlagsdata som input til batchen

    Gitt følgjande innhold i medlemsdata.csv.gz:
      | 1950010112345;0;19500101;12345;666666666666;011;65432109876;;2015.10.14;100.000;60;0;0;0;0;2015.12.01;2345;1 |
      | 1950010112345;0;19500101;12345;666666666666;031;65432109876;;2015.10.14;100.000;60;0;0;0;0;2015.12.31;2345;2 |
      | 1950010112345;1;19500101;12345;666666666666;2015.01.01;2015.12.31;200000;3010                                |
    Og følgjande innhold i spk.csv.gz:
      | SPK_LTR;60;2011.05.01;;600000 |
    Og følgjande innhold i grunnbeloep.csv.gz:
      | OMREGNING;2011.01.01;;90000 |
    Og følgjande innhold i arbeidsgiver.csv.gz:
      | ARBEIDSGIVER;76269;1917.01.01;2014.12.31 |
    Og følgjande innhold i arbeidsgiverdata.csv.gz:
      | KUNDEDATA;28167;123454321;1917.01.01;2014.12.31;76269 |
    Og følgjande innhold i avtale.csv.gz:
      | AVTALE;200000;2015.12.01;;3010;76269 |
    Og følgjande innhold i avtaleversjon.csv.gz:
      | AVTALEVERSJON;200000;2015.12.01;;2014.05.09;AAO-01;FAS;4507292 |
    Og følgjande innhold i avtaleprodukt.csv.gz:
      | AVTALEPRODUKT;200000;PEN;2015.12.01;;10;10.00;2.00;0.35;0.00;0.00;0.00;           |
      | AVTALEPRODUKT;200000;AFP;2015.12.01;;41;02.00;0.00;0.00;0.00;0.00;0.00;           |
      | AVTALEPRODUKT;200000;TIP;1917.01.01;2014.12.31;94;20.00;0.00;0.00;0.00;0.00;0.00; |
      | AVTALEPRODUKT;200000;GRU;2015.12.01;;35;0.00;0.00;0.00;0500.00;0.00;0.00;         |
      | AVTALEPRODUKT;200000;YSK;2015.12.01;;71;0.00;0.00;0.00;2000.00;0.00;0.00;2,5      |

  Scenario: Tilgjengelig modusar
    Gitt at brukaren ønskjer å generere ein tidsserie
    Så skal følgjande modusar vere tilgjengelige for bruk:
      | Navn            | Beskrivelse                                                                                                            |
      | medlemsdatakopi | Ein testmodus som ikkje gjer ein døyt anna enn å generere CSV-filer med alle linjer kopiert inn frå medlemsdata.csv.gz |
      | medlemsid       | Ein testmodus som lar ein inspisere kva trådar medlemmane blir behandla av                                             |

  Scenario: Smoktest med in-process eksekvering
    Gitt at batchen blir køyrt in-process
    Og modus er lik medlemsdatakopi
    Og observasjonsperioda strekker seg frå og med 2015 til og med 2015
    Så skal CSV-filene som blir generert inneholde følgjande rader:
      | medlemsdata                                                                                    |
      | 0 19500101 12345 666666666666 011 65432109876  2015.10.14 100.000 60 0 0 0 0 2015.12.01 2345 1 |
      | 0 19500101 12345 666666666666 031 65432109876  2015.10.14 100.000 60 0 0 0 0 2015.12.31 2345 2 |
      | 1 19500101 12345 666666666666 2015.01.01 2015.12.31 200000 3010                                |

  Scenario: Smoktest med out-of-proces eksekvering
    Gitt at batchen blir køyrt out-of-process
    Og modus er lik medlemsdatakopi
    Og observasjonsperioda strekker seg frå og med 2015 til og med 2015
    Så skal CSV-filene som blir generert inneholde følgjande rader:
      | medlemsdata                                                                                    |
      | 0 19500101 12345 666666666666 011 65432109876  2015.10.14 100.000 60 0 0 0 0 2015.12.01 2345 1 |
      | 0 19500101 12345 666666666666 031 65432109876  2015.10.14 100.000 60 0 0 0 0 2015.12.31 2345 2 |
      | 1 19500101 12345 666666666666 2015.01.01 2015.12.31 200000 3010                                |
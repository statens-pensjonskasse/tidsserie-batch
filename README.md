# PU-FAK-BA-10
## *faktura-tidsserie-batch*
Batchen genererer tidsserier til CSV-format basert på filer generert av faktura-grunnlagsdata-batch.

### Moduler

#### faktura-tidsserie-batch-app
Platformkode som definerer opp fellestjenestene og oppstartsprosedyren som tar seg av validering av input, lagring til CSV-filer og innlesing og opplasting av grunnlagsdata.
Inneholder også main-klassen for batchen og alle kommandolinjeargumenter.

#### faktura-tidsserie-batch-input
Kode for å transformere grunnlagsdata på CSV-format til domene-objekter.

### Overordnet Systemdokumentasjon
[Systemdokumentasjon - faktura-tidsserie-batch (pu_fak_ba_10)](http://wiki/confluence/x/BwMpDQ)

### Driftsdokumentasjon
[Driftsdokumentasjon - faktura-tidsserie-batch (pu_fak_ba_10)](http://wiki/confluence/x/AgMpDQ)

## Ofte spurte spørsmål

# 1. Korleis fiksar eg byggefeil generert av japicmp-maven-plugin?

Viss ein har vore inne og gjort endringar bør ein i så stor grad som mulig, unngå å bryte bakoverkompatibilitet med tidligare versjonar. Dersom ein har vore inne og gjort endringar uten å tenke på dette kan ein derfor fort komme til å ha gjort ei endring som bryter bakoverkompatibiliteten fordi ein har renama, fjerna eller endra på metodeparameter og/eller synligheit.

Sjå [SPK Puma Faktura - Bakoverkompatibilitetspolicy](http://wiki/confluence/display/dok/SPK+Puma+Faktura+-+Bakoverkompatibilitetspolicy) for meir informasjon.

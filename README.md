# PU-FAK-BA-10
## *faktura-tidsserie-batch*
Batchen genererer tidsserier til CSV-format basert på filer generert av faktura-grunnlagsdata-batch.

### Moduler

#### pu-fak-ba-10
Applikasjonsartifakt. Lager .exe fil med embedded java for å kunne kjøre batchen på Windows.
Modulen drar inn app-modulen + alle modusene i en og samme shadede artifact for å sikre at batchen er 100% self contained.

#### pu-fak-ba-11
Script for PU-FAK-BA-11. Ansvarlig for å generere batch-overvåking på vegne av PU-FAK-BA-10, basert på batch.log.
Genererer tar.gz-filen som spkdeploy/spkappctl-deploy.pl deployerer til test- og produksjonsmiljøene og som kjøres via runbatch der.

#### faktura-tidsserie-batch-app
Platformkode som definerer opp fellestjenestene og oppstartsprosedyren som tar seg av validering av input, lagring til CSV-filer og innlesing og opplasting av grunnlagsdata.
Inneholder også main-klassen for batchen og alle kommandolinjeargumenter.

#### faktura-tidsserie-batch-input
Kode for å transformere grunnlagsdata på CSV-format til domene-objekter.

#### faktura-tidsserie-batch-plugin-avregning_tidsserie
Formatdefinisjon og programkode for generering av 1-2 års tidsserier pr stillingsforhold på periodenivå for bruk i forbindelse med årlig avregning av SPK- og Opera-ordningen.
Se [systemdokumentasjon - avregningstidsserie](http://wiki/confluence/display/dok/Systemdokumentasjon+-+PU_FAK_BA_10+-+Modus+-+Avregningstidsserie) for mer informasjon.

#### faktura-tidsserie-batch-plugin-avtaleunderlag
Formatdefinisjon og programkode for generering av tidsserier på avtalenivå for bruk til premiesats- og datakvalitetsformål.
Se [systemdokumentasjon - avtaleunderlag](http://wiki/confluence/display/dok/Systemdokumentasjon+-+PU_FAK_BA_10+-+Modus+-+Avtaleunderlag) for mer informasjon.

#### faktura-tidsserie-batch-plugin-live_tidsserie
Formatdefinisjon og programkode for generering av 10-års tidsserier pr stillingsforhold pr observasjonsdato på periodenivå for bruk i forbindelse med oppfølging av kunder via FFF-dashboard og for årlig- og terminvis prognosegenerering for alle fastsats-avtaler.
Se [systemdokumentasjon - live tidsserie](http://wiki/confluence/display/dok/Systemdokumentasjon+-+PU_FAK_BA_10+-+Modus+-+Live+tidsserie) for mer informasjon.

#### faktura-tidsserie-batch-plugin-stillingsforholdobservasjonar
Formatdefinisjon og programkode for generering av 10-års tidsserier aggregert pr stillingsforhold, avtale og observasjonsdato.Ble brukt i 2015 for prognosegenerering og oppfølging av kunder via FFF-dashboard. Utgår fra og med 2016 da live tidsserie i DVH tar over ansvaret for dette fra denne modusen.
Se [systemdokumentasjon - stillingsforholdobservasjonar](http://wiki/confluence/display/dok/Systemdokumentasjon+-+PU_FAK_BA_10+-+Modus+-+Stillingsforholdobservasjonar) for mer informasjon.

### Overordnet Systemdokumentasjon
[Systemdokumentasjon - faktura-tidsserie-batch (pu_fak_ba_10)](http://wiki/confluence/x/BwMpDQ)

### Driftsdokumentasjon
[Driftsdokumentasjon - faktura-tidsserie-batch (pu_fak_ba_10)](http://wiki/confluence/x/AgMpDQ)

## Ofte spurte spørsmål

# 1. Korleis fiksar eg byggefeil generert av japicmp-maven-plugin?

Viss ein har vore inne og gjort endringar bør ein i så stor grad som mulig, unngå å bryte bakoverkompatibilitet med tidligare versjonar. Dersom ein har vore inne og gjort endringar uten å tenke på dette kan ein derfor fort komme til å ha gjort ei endring som bryter bakoverkompatibiliteten fordi ein har renama, fjerna eller endra på metodeparameter og/eller synligheit.

Sjå [SPK Puma Faktura - Bakoverkompatibilitetspolicy](http://wiki/confluence/display/dok/SPK+Puma+Faktura+-+Bakoverkompatibilitetspolicy) for meir informasjon.

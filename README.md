# PU-FAK-BA-10
## *faktura-tidsserie-batch*
Batchen genererer tidsserier til CSV-format basert p� filer generert av faktura-grunnlagsdata-batch.

### Moduler

#### faktura-tidsserie-batch
Applikasjonsartifakt. Lager .exe fil med embedded java for � kunne kj�re batchen p� Windows.

#### faktura-tidsserie-batch-app
Applikasjonskode. Inneholder koden som generer tidsserier til CSV-format. 

#### faktura-tidsserie-batch-input
Kode for � transformere grunnlagsdata p� CSV-format til domene-objekter.

#### faktura-tidsserie-batch-overvaak
Script for PU-FAK-BA-11. Ansvarlig for � generere batch-overv�king p� vegne av PU-FAK-BA-10, basert p� batch.log.

### Systemdokumentasjon
[Systemdokumentasjon - faktura-tidsserie-batch (pu_fak_ba_10)](http://wiki/confluence/x/BwMpDQ)

### Driftsdokumentasjon
[Driftsdokumentasjon - faktura-tidsserie-batch (pu_fak_ba_10)](http://wiki/confluence/x/AgMpDQ)
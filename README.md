# felles-tidsserie-batch

Platformrammeverk for batchapplikasjonar som ønskjer høgytelses, minnebasert generering av underlags-baserte tidsseriar.

Rammeverket tar seg av:
* Innlasting av medlemsavhengige og/eller medlemsuavhengige grunnlagsdata frå GZIP-komprimerte CSV-filer
* Tilrettelegging for påfølgjande in-memory prosessering av desse, typisk ved hjelp av blant anna [felles-tidsperiode-underlag-lib](http://git.spk.no/projects/FELLESJAVA/repos/felles-tidsperiode-underlag-lib)
* Tilrettelegging for lagring av modus-avhengige tidsrelaterte verdiar til CSV-filer.

Batchapplikasjonar som ønskjer å benytte seg av platformrammeverket er ansvarlig for å:
* Plugge inn funksjonalitet for konvertering av dei medlemsavhengige og -uavhengige grunnlagsdatane frå CSV til tidsperioder klare for prosessering via felles-tidsperiode-underlag-lib.
* Plugge inn Tidsseriemodus(ar) som implementerer den funksjonelle genereringa av resultata som skal produserast, inkludert spesifikasjon og implementasjon av serialiseringa tilbake til CSV-filer.
* Plugge inn kva modusar som skal kunne benyttast
* Plugge inn kva kommandolinjeargument som er tilgjengelig + parsing av desse
* Plugge inn korleis produserte rader skal lagrast tilbake til disk

### Moduler

#### felles-tidsserie-batch-app

Bakoverkompatibel modul som samlar alle påkrevde modular for å få ein køyrbar applikasjon som ein kun treng å legge til 1 eller fleire modusar for å kunne køyre.

#### felles-tidsserie-batch-api

API-modul som definerer kontrakta mellom platformrammeverket og applikasjonar som anvender og pluggar seg inn i rammeverket via tidsseriemodus-konseptet.

Modulen inneheld hovedsaklig grensesnitt for extension pointa som modusane kan plugge seg inn i rammeverket via, f.eks. for å plugge seg inn i livssyklushandteringa til rammeverket viss ein har behov for å klargjere eller rydde opp før/etter tidsseriegenereringa blir køyrt.

Stort sett all integrasjon mellom klientane av rammeverket og platformrammeverket skjer ved hjelp av eit [in-memory tjenesteregister](http://git.spk.no/projects/FF/repos/faktura-tjenesteregister-lib). Modulen inneheld og eit fåtalls støtteklasser for definisjon og bruk av tjenesteregisteret, enten via extension points eller via service locator-mekanisma.

#### felles-tidsserie-batch-main

Implementasjon av platformrammeverket som definerer opp fellestjenestene og oppstartsprosedyren som startar opp rammeverket før modusen overtek og avsluttar rammeverket etter modusen har gjort seg ferdig.
Inneholder main-klassen for platformrammeverket.

#### felles-tidsserie-batch-bom

BOM med dependency management for versjonsstyring av alle modular tilknytta felles-tidsserie-batch.

### Tilgjengelige plugins

#### felles-tidsserie-batch-arguments

Standard kommandolinjegrensesnitt for platformrammeverket. Implementert vha. picocli, kan brukast av applikasjonar som ikkje har behov for fleire/andre kommandolinjeargument enn det minimum som platformrammeverket krever.

#### felles-tidsserie-batch-plugin-disruptor

Standard lagringsmekanisme, implementert via ein ringbuffer vha. LMAX Disruptor for i så stor grad som mulig å unngå køing og synkronisering mellom nodene produserer CSV-rader og I/O-tråden som lagrar radene til disk.

#### felles-tidsserie-batch-plugins-parallelliserte-medlemsdata

Standard medlemsdatabackend, tilbyr in-memory lagring uttrekkets medlemsdata med parallellisert prosessering av desse via tidsseriekommandoen modusen ønskjar å benytte.

#### felles-tidsserie-batch-plugins-konfigurerbar-parallelliserte-medlemsdata

Standard medlemsdatabackend, tilbyr in-memory lagring uttrekkets medlemsdata med parallellisert prosessering av desse via tidsseriekommandoen modusen ønskjar å benytte.
Oppfører seg default likt som felles-tidsserie-batch-plugins-parallelliserte-medlemsdata, men det er mulig å velge forskjellige datalagrings strategier ved å legge det inn i service locator.
Det er også mulig å implementere sine egne løsninger for datalagring på denne måten.

#### felles-tidsserie-batch-plugins-triggerfil

Plugin som genererer ei triggerfil i ut-katalogen kvar gang modusen har køyrt seg ferdig.

Triggerfila blir typisk brukt av eksterne prosessar (f.eks. PowerCenter for DVH) som ønskjer å vente med å starte innlasting av CSV-filenes innhold til køyringa er heilt ferdig.

### Deprekterte plugins

#### felles-tidsserie-batch-plugins-grunnlagsdatavalidator

Inneheld støtte for validering av om uttrekket er konsistent/brukbart før innlasting og køyring av modusen.

Verifiserer at uttrekket inneheld md5-checksums.txt og at MD5-sjekksummen for alle filer matchar det som denne fila inneheld.

#### felles-tidsserie-batch-plugins-metadatawriter

Inneheld støtte for generering av metadata.txt for kvar køyring av batchen.

#### felles-tidsserie-batch-plugins-hazelcast

Medlemsdatabackend implementert vha. Hazelcast IMDG. Har frå versjon 1.2.0 blitt gjort overflødig som følgje av at
felles-tidsserie-batch-plugins-parallelliserte-medlemsdata tilbyr samme funksjonalitet.

Støttar ikkje nyare versjonar av Hazelcast IMDG som følgje av at mekanisma backenden benyttar for distribuert prosessering
har blitt fjerna frå versjon 4 av Hazelcast IMDG.


## Ofte spurte spørsmål

# 1. Korleis fiksar eg byggefeil generert av japicmp-maven-plugin?

Viss ein har vore inne og gjort endringar bør ein i så stor grad som mulig, unngå å bryte bakoverkompatibilitet med tidligare versjonar. Dersom ein har vore inne og gjort endringar uten å tenke på dette kan ein derfor fort komme til å ha gjort ei endring som bryter bakoverkompatibiliteten fordi ein har renama, fjerna eller endra på metodeparameter og/eller synligheit.

Sjå [SPK Puma Faktura - Bakoverkompatibilitetspolicy](http://wiki/confluence/display/dok/SPK+Puma+Faktura+-+Bakoverkompatibilitetspolicy) for meir informasjon.

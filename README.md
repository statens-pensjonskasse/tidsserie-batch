# felles-tidsserie-batch

Platformrammeverk for batchapplikasjonar som ønskjer høgytelses, minnebasert generering av underlags-baserte tidsseriar.

Rammeverket tar seg av innlasting av medlemsavhengige og/eller medlemsuavhengige grunnlagsdata frå GZIP-komprimerte CSV-filer, in-memory prosessering av desse ved hjelp av [felles-tidsperiode-underlag-lib](http://git.spk.no/projects/FELLESJAVA/repos/felles-tidsperiode-underlag-lib) og lagring av genererte underlag og underlagsperioder tilbake til CSV-filer.

Batchapplikasjonar som ønskjer å benytte seg av platformrammeverket blir då ansvarlig for å plugge inn funksjonalitet for konvertering av dei medlemsavhengige og -uavhengige grunnlagsdatane frå CSV til tidsperioder klare for prosessering via felles-tidsperiode-underlag-lib. I tillegg er dei ansvarlige for å plugge inn ein Tidsseriemodus som implementerer den funksjonelle genereringa av underlaga, inkludert spesifikasjon og implementasjon av serialiseringa tilbake til CSV-format for underlagsperiodene som blir generert.


### Moduler

#### felles-tidsserie-batch-app

Platformrammeverk som definerer opp fellestjenestene og oppstartsprosedyren som tar seg av validering av input, lagring til CSV-filer og innlesing og opplasting av grunnlagsdata.
Inneholder også main-klassen for batchen og alle kommandolinjeargumenter.

#### felles-tidsserie-batch-api

API-modul som definerer kontrakta mellom platformrammeverket og applikasjonar som anvender og pluggar seg inn i rammeverket via tidsseriemodus-konseptet.

Modulen inneheld hovedsaklig grensesnitt for extension pointa som modusane kan plugge seg inn i rammeverket via, f.eks. for å plugge seg inn i livssyklushandteringa til rammeverket viss ein har behov for å klargjere eller rydde opp før/etter tidsseriegenereringa blir køyrt.

Stort sett all integrasjon mellom klientane av rammeverket og platformrammeverket skjer ved hjelp av eit [in-memory tjenesteregister](http://git.spk.no/projects/FF/repos/faktura-tjenesteregister-lib). Modulen inneheld og eit fåtalls støtteklasser for definisjon og bruk av tjenesteregisteret, enten via extension points eller via service locator-mekanisma.

#### felles-tidsserie-batch-input

Faktura-spesifikk modul med oversettere frå FFF-relaterte grunnlagsdata til FFF-spesifikke tidsperioder. Batchen vil bli frikobla frå denne før versjon 1.0.0 blir releasa for å unngå kobling til FFF-domenet.

## Ofte spurte spørsmål

# 1. Korleis fiksar eg byggefeil generert av japicmp-maven-plugin?

Viss ein har vore inne og gjort endringar bør ein i så stor grad som mulig, unngå å bryte bakoverkompatibilitet med tidligare versjonar. Dersom ein har vore inne og gjort endringar uten å tenke på dette kan ein derfor fort komme til å ha gjort ei endring som bryter bakoverkompatibiliteten fordi ein har renama, fjerna eller endra på metodeparameter og/eller synligheit.

Sjå [SPK Puma Faktura - Bakoverkompatibilitetspolicy](http://wiki/confluence/display/dok/SPK+Puma+Faktura+-+Bakoverkompatibilitetspolicy) for meir informasjon.

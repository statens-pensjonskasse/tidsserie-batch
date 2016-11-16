# felles-tidsserie-batch

Platformrammeverk for batchapplikasjonar som ønskjer høgytelses, minnebasert generering av underlags-baserte tidsseriar.

Rammeverket tar seg av innlasting av medlemsavhengige og/eller medlemsuavhengige grunnlagsdata frå GZIP-komprimerte CSV-filer, in-memory prosessering av desse ved hjelp av [felles-tidsperiode-underlag-lib](http://git.spk.no/projects/FELLESJAVA/repos/felles-tidsperiode-underlag-lib) og lagring av genererte underlag og underlagsperioder tilbake til CSV-filer.

Batchapplikasjonar som ønskjer å benytte seg av platformrammeverket blir då ansvarlig for å plugge inn funksjonalitet for konvertering av dei medlemsavhengige og -uavhengige grunnlagsdatane frå CSV til tidsperioder klare for prosessering via felles-tidsperiode-underlag-lib. I tillegg er dei ansvarlige for å plugge inn ein Tidsseriemodus som implementerer den funksjonelle genereringa av underlaga, inkludert spesifikasjon og implementasjon av serialiseringa tilbake til CSV-format for underlagsperiodene som blir generert.


### Moduler

#### felles-tidsserie-batch-app

Platformrammeverk som definerer opp fellestjenestene og oppstartsprosedyren som tar seg av validering av input, lagring til CSV-filer og innlesing og opplasting av grunnlagsdata.
Inneholder også main-klassen for batchen og alle kommandolinjeargumenter.

#### felles-tidsserie-batch-input

Faktura-spesifikk modul med oversettere frå FFF-relaterte grunnlagsdata til FFF-spesifikke tidsperioder.


## Ofte spurte spørsmål

# 1. Korleis fiksar eg byggefeil generert av japicmp-maven-plugin?

Viss ein har vore inne og gjort endringar bør ein i så stor grad som mulig, unngå å bryte bakoverkompatibilitet med tidligare versjonar. Dersom ein har vore inne og gjort endringar uten å tenke på dette kan ein derfor fort komme til å ha gjort ei endring som bryter bakoverkompatibiliteten fordi ein har renama, fjerna eller endra på metodeparameter og/eller synligheit.

Sjå [SPK Puma Faktura - Bakoverkompatibilitetspolicy](http://wiki/confluence/display/dok/SPK+Puma+Faktura+-+Bakoverkompatibilitetspolicy) for meir informasjon.

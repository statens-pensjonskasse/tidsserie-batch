# encoding: utf-8
# language: no
Egenskap: Antall feil for en underlagsperiode

  Antall feil i live-tidsserie- og avregnings-formatet for en underlagsperiode
  angir hvor mange regler som ikke kunne beregnes for underlagsperioden.

  Når antall feil er forskjllig fra 0, betyr det at en eller flere av regel-kolonnene i tidsserie-formatet ikke
  kunne beregnes. Det angis ikke hvilke regel som har feilet, og det vil ofte være nødvendig med
  en teknisk gransking av grunnlagsdataene for å avgjøre årsaken til feilen.

  Dersom det er mange underlagsperioder (over halvparten)  som har antall feil forskjellig fra 0, kan dette indikere en systematisk
  feil i beregningsmotoren.

  Eksempler på kolonner som øker antall feil:
  * Regel pensjonsgivende lønn
  * Regel lønnstillegg
  * Regel minstegrense

  Kolonner som angir verdier hentet direkte fra grunnlagsdata vil ikke øke antall feil dersom verdien mangler i underlaget.

  Eksempler på kolonner som *ikke* øker antall feil dersom verdien ikke kunne bestemmes:
  * Orgnummer
  * Ordning
  * Premiestatus

  Antall feil indikerer ikke om hel- og halvfeil som har skjedd i ved innrapportering,
  eller andre logiske feil i grunnlagsdata som er benyttet for å lage tidsserien.

  Scenario: Avregningsformat: Regel som feiler ved beregning, øker antall feil med én.
  Regelen "Er under minstegrense" benytter ordning for å bestemme minstegrense.
  Dersom ordningen mangler, vil beregning av minstegrense feile, og antall feil økes med en.
    Gitt en villkårlig underlagsperiode
    Når underlaget formateres med avregningsformat
    Og kun følgende kolonner beregnes:
      | Kolonnenavn                  |
      | FLG_REGEL_UNDER_MINSTEGRENSE |
      | NUM_ANTALLFEIL               |
    Så blir resultatet med angitt format:
      | FLG_REGEL_UNDER_MINSTEGRENSE | NUM_ANTALLFEIL |
      |                              | 1              |

  Scenario: Avregningsformat: Hente en verdi som ikke finnes i underlagsperioden, fører ikke til feil
    Gitt en villkårlig underlagsperiode
    Når underlaget formateres med avregningsformat
    Og kun følgende kolonner beregnes:
      | Kolonnenavn    |
      | IDE_PENS_ORD   |
      | NUM_ANTALLFEIL |
    Så blir resultatet med angitt format:
      | IDE_PENS_ORD | NUM_ANTALLFEIL |
      |              | 0              |

  Scenario: Live-tidsserieformat: Regel som feiler ved beregning, øker antall feil med én.
  Regelen "Er under minstegrense" benytter ordning for å bestemme minstegrense.
  Dersom ordningen mangler, vil beregning av minstegrense feile, og antall feil økes med en.
    Gitt en villkårlig underlagsperiode
    Når underlaget formateres med live-tidsserieformat
    Og kun følgende kolonner beregnes:
      | Kolonnenavn                |
      | regel_erUnderMinstegrensen |
      | antallFeil                 |
    Så blir resultatet med angitt format:
      | regel_erUnderMinstegrensen | antallFeil |
      |                            | 1          |

  Scenario: Live-tidsserieformat: Hente en verdi fra underlagsperiode som ikke finnes i perioden, fører ikke til feil
    Gitt en villkårlig underlagsperiode
    Når underlaget formateres med live-tidsserieformat
    Og kun følgende kolonner beregnes:
      | Kolonnenavn |
      | ordning     |
      | antallFeil  |
    Så blir resultatet med angitt format:
      | ordning | antallFeil |
      |         | 0          |
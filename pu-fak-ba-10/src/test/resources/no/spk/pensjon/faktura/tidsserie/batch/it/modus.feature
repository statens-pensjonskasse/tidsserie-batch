# language: no
# encoding: UTF-8

Egenskap: Tidsseriar generert for kvar av modusane

  Batchen faktura-tidsserie-batch tilbyr fleire modusar som brukaren kan be om å få generert tidsseriar via.

  Bakgrunn: Grunnlagsdata for eit medlem og påkrevde referansedata

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
    Og følgjande innhold i avregningsperioder.csv.gz:
      | AVREGNINGSPERIODE;2015;2015;1 |
    Og følgjande innhold i avregningsavtaler.csv.gz:
      | AVREGNINGSAVTALE;2015;2015;1;200000 |
    Og batchen blir køyrt out-of-process


  Scenario: Tilgjengelig modusar
    Gitt at brukaren ønskjer å generere ein tidsserie
    Så skal følgjande modusar vere tilgjengelige for bruk:
      | Navn                          | Beskrivelse                                                                                                                                |
      | stillingsforholdobservasjonar | Ein (typisk) 10-årig tidsserie beståande av opp til 12 observasjonar pr stilling pr år                                                     |
      | avregning_tidsserie           | Ein 1 til 2-årig tidsserie som blir brukt til utregning av riktig årspremie for avtalar som skal avregnast for eit eller fleire premieår   |
      | live_tidsserie                | Ein meir detaljert versjon av stillingsforholdobservasjonar, består av alle underlagsperiodene som den første er eit aggregert resultat av |
      | avtaleunderlag                | En tidsserie som periodiserer avtaler og som benyttes for å bestemme premiesatser for disse.                                               |

  Scenario: Tidsserie aggregert til stillingsforholdnivå

    Gitt at modus er lik stillingsforholdobservasjonar
    Og observasjonsperioda strekker seg frå og med 2015 til og med 2015
    Og følgjande kolonner blir ignorert fordi dei endrar verdi frå køyring til køyring:
      | uuid            |
      | tidsserienummer |
    Så skal CSV-filene som blir generert inneholde følgjande rader:
      | avtaleId | stillingsforholdId | observasjonsdato | maskinelt_grunnlag | premiestatus | årsverk | personnummer |
      | 200000   | 666666666666       | 2015-12-31       | 50959              | AAO-01       | 0.085   | MISSING      |

  Scenario: Live tidsserie

    Gitt at modus er lik live_tidsserie
    Og observasjonsperioda strekker seg frå og med 2015 til og med 2015
    Og følgjande kolonner blir ignorert fordi dei endrar verdi frå køyring til køyring:
      | uuid            |
      | tidsserienummer |
    Så skal CSV-filene som blir generert inneholde følgjande rader:
      | observasjonsdato | fraOgMedDato | tilOgMedDato | medlem        | stillingsforhold | avtale | organisasjonsnummer | ordning | premiestatus | aksjonskode | stillingskode | stillingsprosent | loennstrinn | loennstrinnBeloep | deltidsjustertLoenn | fasteTillegg | variableTillegg | funksjonsTillegg | medregning | medregningskode | grunnbeloep | regel_aarsfaktor | regel_aarslengde | regel_aarsverk | regel_antalldager | regel_deltidsjustertloenn | regel_loennstillegg | regel_pensjonsgivende_loenn | regel_medregning | regel_minstegrense | regel_oevreLoennsgrense | regel_gruppelivsandel | regel_yrkesskadeandel | regel_erMedregning | regel_erPermisjonUtenLoenn | regel_erUnderMinstegrensen | produkt_PEN | produkt_PEN_satsArbeidsgiver | produkt_PEN_satsMedlem | produkt_PEN_satsAdministrasjonsgebyr | produkt_PEN_produktinfo | produkt_AFP | produkt_AFP_satsArbeidsgiver | produkt_AFP_satsMedlem | produkt_AFP_satsAdministrasjonsgebyr | produkt_AFP_produktinfo | produkt_TIP | produkt_TIP_satsArbeidsgiver | produkt_TIP_satsMedlem | produkt_TIP_satsAdministrasjonsgebyr | produkt_TIP_produktinfo | produkt_GRU | produkt_GRU_satsArbeidsgiver | produkt_GRU_satsMedlem | produkt_GRU_satsAdministrasjonsgebyr | produkt_GRU_produktinfo | produkt_YSK | produkt_YSK_satsArbeidsgiver | produkt_YSK_satsMedlem | produkt_YSK_satsAdministrasjonsgebyr | produkt_YSK_produktinfo | produkt_YSK_risikoklasse | antallFeil | arbeidsgivernummer | termintype | linjenummer_historikk | premiekategori |
      | 2015-12-31       | 2015-12-01   | 2015-12-31   | 1950010112345 | 666666666666     | 200000 |                     | 3010    | AAO-01       | 011         | 2345          | 100.000          | 60          | 600000            | 0                   |              |                 |                  |            |                 | 90000       | 8.49315068       | 365              | 8.49           | 31                | 600000                    | 0                   | 50959                       | 0                | 20.00              | 1080000                 | 100.0000              | 100.0000              | 0                  | 0                          | 0                          | 1           | 10.00                        | 2.00                   | 0.35                                 | 10                      | 1           | 2.00                         | 0.00                   | 0.00                                 | 41                      |             |                              |                        |                                      |                         | 1           | 500                          | 0                      | 0                                    | 35                      | 1           | 2000                         | 0                      | 0                                    | 71                      | 2,5                      | 0          |                    | SPK        | 1                     | FAS            |

  Scenario: Tidsserie på periodenivå for avregning
    Gitt at modus er lik avregning_tidsserie
    Og observasjonsperioda strekker seg frå og med 2015 til og med 2015
    Og følgjande kolonner blir ignorert fordi dei endrar verdi frå køyring til køyring:
      | IDE_UUID            |
      | IDE_TIDSSERIENUMMER |
    Så skal CSV-fila som blir generert inneholde følgjande rader:
      | DAT_OBSERVASJON | DAT_FOM    | DAT_TOM    | DAT_KUNDE_FOEDT_NUM | IDE_KUNDE_PRSNR | IDE_SEKV_TORT125 | NUM_AVTALE_ID | IDE_ARBGIV_NR | IDE_PENS_ORD | IDE_AVTALE_PREMIEST | TYP_AKSJONSKODE | NUM_STILLINGSKODE | RTE_DELTID | NUM_LTR | BEL_LONN | BEL_DELTIDSJUSTERT_LONN | BEL_FTILL | BEL_VTILL | BEL_FUTILL | BEL_LONN_MDR | TYP_KODE_MDR | BEL_G | RTE_REGEL_AARSFAKTOR | NUM_REGEL_AARSLENGDE | RTE_REGEL_AARSVERK | NUM_REGEL_ANTALLDAGER | BEL_REGEL_DELTIDSJUSTERT_LONN | BEL_REGEL_LONN_TILLEGG | BEL_REGEL_PENSJONSGIVENDE_LONN | BEL_REGEL_MEDREGNING | RTE_REGEL_MINSTEGRENSE | NUM_REGEL_OEVRE_LONNSGRENSE | RTE_REGEL_GRUPPELIV | RTE_REGEL_YRKESSKADE | FLG_REGEL_MEDREGNING | FLG_REGEL_PERMISJON_UTEN_LONN | FLG_REGEL_UNDER_MINSTEGRENSE | FLG_PEN | RTE_PEN_ARBANDEL | RTE_PEN_MEDL_ANDEL | RTE_PEN_ADMGEB | KOD_PEN_PRODUKTINFO | FLG_AFP | RTE_AFP_ARBANDEL | RTE_AFP_MEDL_ANDEL | RTE_AFP_ADMGEB | KOD_AFP_PRODUKTINFO | FLG_TIP | RTE_TIP_ARBANDEL | RTE_TIP_MEDL_ANDEL | RTE_TIP_ADMGEB | KOD_TIP_PRODUKTINFO | FLG_GRU | BEL_GRU_ARBANDEL | BEL_GRU_MEDL_ANDEL | BEL_GRU_ADMGEB | KOD_GRU_PRODUKTINFO | FLG_YSK | BEL_YSK_ARBANDEL | BEL_YSK_MEDL_ANDEL | BEL_YSK_ADMGEB | KOD_YSK_PRODUKTINFO | KOD_YSK_RISIKO_KL | NUM_ANTALLFEIL | IDE_SEKV_TORT129 | KOD_TERMINTYPE | IDE_LINJE_NR | TYP_PREMIEKATEGORI | IDE_AVREGNING_VERSJON | BEL_PEN_PREMIE_MEDL | BEL_PEN_PREMIE_ARBGIV | BEL_PEN_PREMIE_ADM_GEB | BEL_AFP_PREMIE_MEDL | BEL_AFP_PREMIE_ARBGIV | BEL_AFP_PREMIE_ADM_GEB | BEL_TIP_PREMIE_MEDL | BEL_TIP_PREMIE_ARBGIV | BEL_TIP_PREMIE_ADM_GEB | BEL_GRU_PREMIE_MEDL | BEL_GRU_PREMIE_ARBGIV | BEL_GRU_PREMIE_ADM_GEB | BEL_YSK_PREMIE_MEDL | BEL_YSK_PREMIE_ARBGIV | BEL_YSK_PREMIE_ADM_GEB | KOD_TILGANGSGRUPPE | NUM_ANTALLDAGSVERK_GRU | NUM_ANTALLDAGSVERK_YSK | KOD_FORDELINGSAARSAK_GRU | KOD_FORDELINGSAARSAK_YSK |
      | 2015-12-31      | 2015-12-01 | 2015-12-31 | 19500101            | 12345           | 666666666666     | 200000        |               | 3010         | AAO-01              | 011             | 2345              | 100.000    | 60      | 600000   | 0                       |           |           |            |              |              | 90000 | 8.4931507            | 365                  | 8.49               | 31                    | 600000                        | 0                      | 50959                          | 0                    | 35.00                  | 1080000                     | 100.0000            | 100.0000             | 0                    | 0                             | 0                            | 1       | 10.00            | 2.00               | 0.35           | 10                  | 1       | 2.00             | 0.00               | 0.00           | 41                  |         |                  |                    |                |                     | 1       | 500              | 0                  | 0              | 35                  | 1       | 2000             | 0                  | 0              | 71                  | 2,5               | 0              |                  | SPK            | 1            | FAS                | 1                     | 1019.18             | 5095.90               | 178.36                 | 0.00                | 1019.18               | 0.00                   | 0.00                | 0.00                  | 0.00                   | 0.00                | 0.00                  | 0.00                   | 0.00                | 0.00                  | 0.00                   |                    | 31.00000                | 31.00000                | ORD                      | ORD                      |

  Scenario: Avtaleunderlag
    Gitt at modus er lik avtaleunderlag
    Og observasjonsperioda strekker seg frå og med 2015 til og med 2015
    Og følgjande kolonner blir ignorert fordi dei endrar verdi frå køyring til køyring:
      | uuid            |
      | tidsserienummer |
      | uttrekksdato    |
    Så skal CSV-filene som blir generert inneholde følgjande rader:
      | premieaar | fraOgMedDato | tilOgMedDato | avtale | antallFeil | regel_aarsfaktor | regel_aarslengde | regel_antalldager | ordning | arbeidsgivernummer | organisasjonsnummer | premiestatus | premiekategori | produkt_PEN | produkt_PEN_satsArbeidsgiver | produkt_PEN_satsMedlem | produkt_PEN_satsAdministrasjonsgebyr | produkt_PEN_satsTotal | produkt_PEN_produktinfo | produkt_AFP | produkt_AFP_satsArbeidsgiver | produkt_AFP_satsMedlem | produkt_AFP_satsAdministrasjonsgebyr | produkt_AFP_satsTotal | produkt_AFP_produktinfo | produkt_TIP | produkt_TIP_satsArbeidsgiver | produkt_TIP_satsMedlem | produkt_TIP_satsAdministrasjonsgebyr | produkt_TIP_satsTotal | produkt_TIP_produktinfo | produkt_prosent_satsTotal | produkt_GRU | produkt_GRU_satsArbeidsgiver | produkt_GRU_satsMedlem | produkt_GRU_satsAdministrasjonsgebyr | produkt_GRU_satsTotal | produkt_GRU_produktinfo | produkt_YSK | produkt_YSK_satsArbeidsgiver | produkt_YSK_satsMedlem | produkt_YSK_satsAdministrasjonsgebyr | produkt_YSK_satsTotal | produkt_YSK_produktinfo | produkt_YSK_risikoklasse |
      | 2015      | 2015-12-01   | 2015-12-31   | 200000 | 0          | 8.49315068       | 365              | 31                | 3010    | 76269              |                     | AAO-01       | FAS            | 1           | 10.00                        | 2.00                   | 0.35                                 | 12.35                 | 10                      | 1           | 2.00                         | 0.00                   | 0.00                                 | 2.00                  | 41                      |             |                              |                        |                                      |                       |                         | 14.35                     | 1           | 500                          | 0                      | 0                                    | 500                   | 35                      | 1           | 2000                         | 0                      | 0                                    | 2000                  | 71                      | 2,5                      |

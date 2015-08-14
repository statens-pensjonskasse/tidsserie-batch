#!/bin/bash

#===========================================================
# pu-fak-ba-09.sh
#===========================================================
# Beskrivelse:
# Kjoerer faktura_tidsserie-batch-overvaak
#===========================================================

#===========================================================
# Paakrevde miljovariabler maa vaere satt foer batch kalles
#===========================================================
# SPK_DATA: Datapath for batch-packaging projektet.
# SPK_SCRIPT: Path som angir hvor shell skriptene ligger

#===========================================================
# Deklarasjon av variable + evt. debug
#===========================================================
#Initialiseringer
STATUS=0															  # OK (0=OK, 1=Warning (Kjørt ferdig, men noe feilet), 2=Error)
COMMENT=""															  # Kommentar til kjøringen. Kan ende opp som tom, om alt gaar bra.

#Batch-spesifikke settings
#JVM_ARGS="-Dcom.sun.management.jmxremote.port=9876 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -XX:+UseConcMarkSweepGC -Xms512m -Xmx2g -XX:MaxPermSize=1g"
APPLIKASJONSGRUPPE=faktura
SERVICENAME=$(basename $0 .sh)                                        # Navn på batch i batchovervåkingen utledet fra navnet på batch skriptet
WORKDIR=${SPK_DATA}/puma/${APPLIKASJONSGRUPPE}/${SERVICENAME}		  # Arbeidsomrade for batchen. (inn/ut/log etc.)
HOMEDIR=$(dirname $0)      		                                      # Katalog hvor shell skriptet kjøres fra
READ_LOG=/C:/prognose/tidsserie/batch.log
#Beregnede settings
SCRIPT_NAME=${0##*/}
LOGDIR=${WORKDIR}/log                                 # Omrade hvor log filer opprettes

#===========================================================
# Deklarasjon av funksjoner
#===========================================================
source /usr/local/bin/common.sh
source ${SPK_SCRIPT}/felles/common_batch.sh

Usage() {
    echo "Stuff"
}

#===========================================================
# Hovedprogram
#===========================================================
# Om batchen ble kalt med parameter -h for help viser vi hjelpetekst og avslutter
# Batchen gjor annen parametervalidering selv: derfor kjores getops i silent mode (':' som forste tegn)
while getopts ":h" OPT ; do
    case "$OPT" in
        h)
            Usage
            exit;;
    esac
done

ExitIfNotExistsSPK_DATA
CreateRequiredCatalogs ${LOGDIR}

# KJØR BATCH
cd ${HOMEDIR}

LogStart "Startet."

# Henter ut ID til den oppdaterte TORT901-innslaget lagd av LogStart
CURRENT_IDE_SEKV_TORT901=$IDE_SEKV_TORT901
echo "Kjoerer batch med java -jar $(GetJarValue)"


CMDRESULT=$?

if [[ $CMDRESULT -ne 0 ]] ; then
	COMMENT="Feil ved kjoering av ${SERVICENAME}!"
	UpdateLogStartIfBatchFailed $CURRENT_IDE_SEKV_TORT901 "$COMMENT"
else
    LogFinish $CMDRESULT
	LogInfo "Fullført OK"
fi
exit $CMDRESULT


#!/bin/bash

#===========================================================
# pu-fak-ba-11.sh
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
APPLIKASJONSGRUPPE=faktura
SERVICENAME=$(basename $0 .sh)                                        # Navn på batch i batchovervåkingen utledet fra navnet på batch skriptet
OVERVAAKING_SERVICENAME=${SERVICENAME}
WORKDIR=${SPK_DATA}/puma/${APPLIKASJONSGRUPPE}/${SERVICENAME}		  # Arbeidsomrade for batchen. (inn/ut/log etc.)
HOMEDIR=$(dirname $0)      		                                      # Katalog hvor shell skriptet kjøres fra
#Beregnede settings
SCRIPT_NAME=${0##*/}
LOGDIR=${WORKDIR}                                                     # Omrade hvor log filer opprettes
MASTERLOG=${WORKDIR}/master.log                                       # Loggfil for dette scriptet. Brukes av logging til batchovervåking
PU_FAK_BA_10_LOGDIR=${SPK_DATA}/puma/faktura/pu_fak_ba_10

#===========================================================
# Deklarasjon av funksjoner
#===========================================================
source /usr/local/bin/common.sh
source ${SPK_SCRIPT}/felles/common_batch.sh

Usage() {
    echo "pu_fak_ba_11.sh"
}

FindLatestLog() {
    # Finn alle tidsserie-batch-logkataloger i PU_FAK_BA_10_LOGDIR, sorter og hent ut første (nyeste kjøring)
    local last_batch_log=$(find ${PU_FAK_BA_10_LOGDIR} -maxdepth 2 -name "batch.log" | sort -r | head -n 1)

    if [[ ! -f ${last_batch_log} ]] ; then
        ExitWithError 2 "Fant ikke batch.log i underkataloger av ${PU_FAK_BA_10_LOGDIR}."
        exit 1
    fi

    echo ${last_batch_log}
}

GetHostname(){
    local log_line=$1
    local hostname_pattern='(?<=HOSTNAME":").+?(?=")'
    echo ${first_log_line} | grep -oP "${hostname_pattern}"
}

GetTimeForLine(){
    local log_line=$1
    local timestamp_pattern='(?<=timestamp":")\d{4}-\d{2}-\w{5}:\d{2}:\d{2}'
    echo ${log_line} | grep -oP "${timestamp_pattern}" | sed -e 's/T/ /g'
}

GetExitCode(){
    local log_file=$1
    local exitcode_pattern='(?<=Exit code: )(\d)'
    echo $(grep -oP "${exitcode_pattern}" "${log_file}")
}

GetResultat(){
    local log_file=$1
    local result_pattern='(?<=Resultat av k\\u00F8yring: {)(.*?)(?=})'
    echo $(grep -oP "${result_pattern}" "${last_batch_log}")
}

GetResultatMedlemmer(){
    local resultat=$1
    local medlem_pattern='(?<=medlem=)(\d+)'
    echo $(echo "${resultat}" | grep -oP "${medlem_pattern}")
}

GetResultatErrors(){
    local resultat=$1
    local errors_pattern='(?<=errors=)(\d+)'
    echo $(echo "${resultat}" | grep -oP "${errors_pattern}")
}

LogComment(){
    local batch_exit_code=$1
    local logComment

    if [[ -z ${batch_exit_code}  ]] ; then
        STATUS=2
        logComment="En uventet feil oppstod. Informer Team Kornmo."
    elif [[ ${batch_exit_code} -eq 0 ]] ; then
        STATUS=0
        logComment="Tidsserie generert OK."
    elif [[ ${batch_exit_code} -eq 1 ]] ; then
        STATUS=2
        logComment="Tidsserie-generering feilet. Informer Team Kornmo."
    else
        STATUS=2
        logComment="En uventet feil oppstod. Informer Team Kornmo."
    fi

    if [[ -n ${batch_medlem} ]] ; then
        logComment="$logComment Antall medlemmer: ${batch_medlem}."
    fi

    if [[ -n ${batch_errors} ]] ; then
        logComment="$logComment Antall feil: ${batch_errors}."
    fi

    echo ${logComment}
}

RunSql() {
    echo $(echo -e "$1" | syb_sql_run.sh -clean)
}

RunInsertSql() {
    if ! IDE_SEKV_TORT901=$(echo -e "$1" | syb_sql_run.sh -clean) ; then
            LOG_START_COMMENT="Innsetting av status i $DB_DATABASE.tort901 på server $DB_SERVER med bruker $DB_USER for kjøring \"$SERVICENAME\" feilet! Kommentar fra kjørescript: \"$LOG_START_COMMENT\""
            SERVICENAME=${OVERVAAKING_SERVICENAME}
            LogFatal "$LOG_START_COMMENT"
    fi
}

ReplaceSpkDataPath() {
    echo $(echo ${1} | sed -e "s|${SPK_DATA}|SPK_DATA|g")
}

#===========================================================
# Hovedprogram
#===========================================================
# Om batchen ble kalt med parameter -h for help viser vi hjelpetekst og avslutter
while getopts "h" OPT ; do
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

LogStart "Leser inn logfiler fra pu_fak_ba_10."
# Henter ut ID til den oppdaterte TORT901-innslaget lagd av LogStart
CURRENT_IDE_SEKV_TORT901=$IDE_SEKV_TORT901

# Finn alle tidsserie-batch-logkataloger i PU_FAK_BA_10_LOGDIR, sorter og hent ut første (nyeste kjøring)
last_batch_log=$(FindLatestLog)

first_log_line=$(head -n 1 ${last_batch_log})

batch_hostname=$(GetHostname "${first_log_line}")
batch_start=$(GetTimeForLine "${first_log_line}")
batch_end=$(GetTimeForLine "$(tail -n 1 "${last_batch_log}")")
batch_exit_code=$(GetExitCode "${last_batch_log}")
batch_result=$(GetResultat "${last_batch_log}")
batch_medlem=$(GetResultatMedlemmer "${batch_result}")
batch_errors=$(GetResultatErrors "${batch_result}")

batch_log_path_short=$(ReplaceSpkDataPath ${last_batch_log})

# Dersom start/stopp/hostname ikke er lesbart fra filen, er det for lite info til aa lage PU_FAK_BA_10 overvaaking.
if [[ -z "$batch_hostname" || -z "$batch_start" || -z "$batch_end" ]] ; then
    ExitWithError 2 "'${batch_log_path_short}' kunne ikke leses, eller inneholder ufullstendig informasjon."
    exit 1
fi

check_sql="select distinct 1 from tort901 where nvn_tjeneste = 'PU_FAK_BA_10' and dat_start = '$batch_start'"
batch_exists=$(RunSql "${check_sql}")

# Lager kun PU_FAK_BA_10 rad i tort901 dersom starttidspunktet ikke finnes fra foer.
if [[ -z "$batch_exists" ]] ; then
    ##Imiterer PU_FAK_BA_10 for overvaaking
    SERVICENAME="PU_FAK_BA_10"

    LOG_START_COMMENT=$(LogComment ${batch_exit_code})

    sql="insert into tort901 (nvn_maskin, nvn_tjeneste, dat_start, dat_slutt, sta_kjoring, txt_fri) values ('$batch_hostname', '$SERVICENAME', '$batch_start', '$batch_end', '$STATUS', '$LOG_START_COMMENT')\ngo\nselect @@identity"
    RunInsertSql "${sql}"

    SERVICENAME=${OVERVAAKING_SERVICENAME}
    ExitOk 0 "Avsluttet OK. PU_FAK_BA_10-log '${batch_log_path_short}' ble behandlet."
else
    ExitOk 0 "Avsluttet OK. PU_FAK_BA_10-log '${batch_log_path_short}' var allerede behandlet."
fi
exit 0


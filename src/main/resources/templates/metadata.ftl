<#-- @ftlvariable name="outputDirectory" type="java.lang.String" -->
<#-- @ftlvariable name="jobDuration" type="java.lang.String" -->
<#-- @ftlvariable name="params" type="no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments" -->
Batch-id: ${batchId}
Beskrivelse: ${params.beskrivelse}

<#--Status: ${job.exitStatus.exitCode}-->
<#--Startet: ${job.startTime?string["dd.MM.yyyy - HH:mm:ss"]}-->
<#--Avsluttet: ${job.endTime?string["dd.MM.yyyy - HH:mm:ss"]}-->
<#--Kjøretid: ${jobDuration}-->

Batch parametere:
* Fra og med år: ${params.fraAar}
* Til og med år: ${params.tilAar}
* Arbeidskatalog: ${outputDirectory}
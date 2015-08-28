<#-- @ftlvariable name="outputDirectory" type="java.lang.String" -->
<#-- @ftlvariable name="jobDuration" type="java.lang.String" -->
<#-- @ftlvariable name="params" type="no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments" -->
Batch-id: ${batchId}
Beskrivelse: ${params.beskrivelse}

<#--Status: ${job.exitStatus.exitCode}-->
<#--Startet: ${job.startTime?string["dd.MM.yyyy - HH:mm:ss"]}-->
<#--Avsluttet: ${job.endTime?string["dd.MM.yyyy - HH:mm:ss"]}-->
<#--Kj�retid: ${jobDuration}-->

Batch parametere:
* Fra og med �r: ${params.fraAar}
* Til og med �r: ${params.tilAar}
* Grunnlagsdata batch-id: ${params.grunnlagsdataBatchId}
* Grunnlagsdata batch-katalog: ${params.grunnlagsdataBatchKatalog.toAbsolutePath().normalize()}
* Arbeidskatalog: ${outputDirectory}
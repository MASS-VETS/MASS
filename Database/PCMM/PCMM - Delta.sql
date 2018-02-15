SELECT

--
-- Patient Information
pr.PatIdentifier, --Local patient identifier
pr.StationID, --IEN for the Facility
ISNULL(pr.PatLastName,'') PatLastName, -- Patient Name as seperate columns to make transformation to HL7 simpler.
ISNULL(pr.PatFirstName,'') PatFirstName,
ISNULL(pr.PatMiddleName,'') PatMiddleName,
CASE WHEN TRY_CONVERT(DATETIME,pr.PatBirthDate,102) IS NULL THEN '' ELSE FORMAT(TRY_CONVERT(DATETIME,pr.PatBirthDate,102), N'yyyyMMdd') END PatBirthDate,
pr.PatGender,

--
-- Provider Information
ISNULL(pr.TeamRole,'') TeamRole,
pr.StaffStation,
pr.StaffIdentifier,
ISNULL(pr.StaffLastName,'') StaffLastName,
ISNULL(pr.StaffFirstName,'') StaffFirstName,
ISNULL(pr.StaffMiddleName,'') StaffMiddleName,

-- Association Start and End Dates
CASE WHEN pr.AssocStart IS NULL THEN '' ELSE FORMAT(pr.AssocStart, N'yyyyMMddHHmmss') END AssocStart,
CASE WHEN pr.AssocEnd IS NULL THEN '' ELSE FORMAT(pr.AssocEnd, N'yyyyMMddHHmmss') END AssocEnd

FROM

[PCMM].[MASS_PATIENT_PROVIDER_RELATIONSHIP] pr

WHERE  

pr.TEAM_ASSIGNMENT_STATUS IN ('Active','Inactive') AND --All changes active or inactive
pr.LAST_MODIFIED_DATE > DATEADD(hh,-28,GETDATE()) AND --Which have happened in the last 25 hours.
pr.StationID = '757' --For the columbus site.

ORDER BY pr.PCMM_PATIENT_ID
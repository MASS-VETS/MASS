--
-- Set the date for the query here so that we don't have to compute it multiple times.

DECLARE @afterDate DATETIME
SET @afterDate = DATEADD(hh,-25,GETDATE())

--Remove above for a full query
--

SELECT

--
-- Patient Information
ISNULL(vPat.DFN,'') PatIdentifier, --Local patient identifier
ISNULL(vPat.STA3N,'') StationID, --IEN for the Facility

ISNULL(pat.LAST_NAME,'') PatLastName, -- Patient Name as seperate columns to make transformation to HL7 simpler.
ISNULL(pat.FIRST_NAME,'') PatFirstName,
ISNULL(pat.MIDDLE_NAME,'') PatMiddleName,

FORMAT(ISNULL(pat.BIRTH_DATE,''), N'yyyyMMdd') PatBirthDate,
ISNULL(pat.GENDER_ID,'') PatGender,

--
-- Provider Information
ISNULL(teamRole.NAME,'') TeamRole,
ISNULL(staff.STA3N,'') StaffStation,
ISNULL(staff.STAFF_IEN,'') StaffIdentifier,
ISNULL(staff.LAST_NAME,'') StaffLastName,
ISNULL(staff.FIRST_NAME,'') StaffFirstName,
ISNULL(staff.MIDDLE_NAME,'') StaffMiddleName,

-- Association Start and End Dates
FORMAT(ISNULL(teamPatAssign.START_DATE,''), N'yyyyMMddHHmmss') AssocStart,
FORMAT(ISNULL(teamPatAssign.END_DATE,''), N'yyyyMMddHHmmss') AssocEnd


FROM

PCMM.PCMM_VISTA_PATIENT vPat

--
-- Join all of the necessary tables.
JOIN
PCMM.PCMM_PATIENT pat
ON
vPat.PCMM_PATIENT_ID = pat.PCMM_PATIENT_ID

JOIN
PCMM.TEAM_PATIENT_ASSIGN teamPatAssign
ON
pat.PCMM_PATIENT_ID = teamPatAssign.PCMM_PATIENT_ID
AND
vPat.STA3N = teamPatAssign.STA3N

JOIN
PCMM.TEAM team
ON
team.TEAM_ID = teamPatAssign.TEAM_ID

JOIN
PCMM.PCM_STD_TEAM_CARE_TYPE teamCareType
ON
team.PCM_STD_TEAM_CARE_TYPE_ID = teamCareType.PCM_STD_TEAM_CARE_TYPE_ID

JOIN
PCMM.TEAM_MEMBERSHIP teamMembership
ON
team.TEAM_ID = teamMembership.TEAM_ID

JOIN
PCMM.TEAM_POSITION teamPos
ON
teamPos.TEAM_POSITION_ID = teamMembership.TEAM_POSITION_ID

JOIN
PCMM.PCM_STD_TEAM_ROLE teamRole
ON
teamRole.PCM_STD_TEAM_ROLE_ID = teamPos.PCM_STD_TEAM_ROLE_ID

JOIN
PCMM.STAFF staff
ON
staff.STAFF_ID = teamMembership.STAFF_ID

--
-- Selection Criteria

WHERE
--Use station ID
vPat.STA3N = '757'

--
--Remove the following for the full load query

AND (
vPat.RECORD_MODIFIED_DATE > @afterDate
OR 
pat.RECORD_MODIFIED_DATE > @afterDate
OR
teamPatAssign.RECORD_MODIFIED_DATE > @afterDate
OR
teamMembership.RECORD_MODIFIED_DATE > @afterDate
OR
teamPos.RECORD_MODIFIED_DATE > @afterDate
)

ORDER BY vPat.DFN


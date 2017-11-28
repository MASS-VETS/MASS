
SELECT

--
-- Patient Information
ISNULL(vPat.DFN,'') PatIdentifier, --Local patient identifier
ISNULL(vPat.STA3N,'') StationID, --IEN for the Facility

ISNULL(pat.LAST_NAME,'') PatLastName, -- Patient Name as seperate columns to make transformation to HL7 simpler.
ISNULL(pat.FIRST_NAME,'') PatFirstName,
ISNULL(pat.MIDDLE_NAME,'') PatMiddleName,

CASE WHEN TRY_CONVERT(DATETIME,pat.BIRTH_DATE,102) IS NULL THEN '' ELSE FORMAT(TRY_CONVERT(DATETIME,pat.BIRTH_DATE,102), N'yyyyMMdd') END PatBirthDate,
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
CASE WHEN teamPatAssign.START_DATE IS NULL THEN '' ELSE FORMAT(teamPatAssign.START_DATE, N'yyyyMMddHHmmss') END AssocStart,
CASE WHEN teamPatAssign.END_DATE IS NULL THEN '' ELSE FORMAT(teamPatAssign.END_DATE, N'yyyyMMddHHmmss') END AssocEnd

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
vPat.STA3N = '757'  --Use station ID
AND
teamPatAssign.END_DATE IS NULL -- Make sure that the assignment is active.

ORDER BY vPat.DFN

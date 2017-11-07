-- Database Creation Script for Adapter
-- Confirm that you have selected the database you wish to make additions to.

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

--Drop all of the existing tables
DROP TABLE keyValues
DROP TABLE messageData
DROP TABLE interfaces

--Drop all of the stored procedures
DROP PROCEDURE purgeMessageData
DROP PROCEDURE storeKeyValue
DROP PROCEDURE storeMessage
DROP PROCEDURE storeHAPIKeyValue
DROP PROCEDURE storeHAPIMessage

--First create the interfaces table

CREATE TABLE [dbo].[interfaces](
	[ID] [uniqueidentifier] ROWGUIDCOL  NOT NULL,
	[Name] [nvarchar](100) NOT NULL,
	[Direction] [nvarchar](20) NOT NULL,
	[PurgeDays] [tinyint] NULL,
 CONSTRAINT [PK_interfaces] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[interfaces] ADD  CONSTRAINT [DF_interfaces_ID]  DEFAULT (newid()) FOR [ID]
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'List of connection interfaces which can send or receive data in the adapter.' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'interfaces'
GO


--Then Message Data so that we have keys available for reference

CREATE TABLE [dbo].[messageData](
	[ID] [uniqueidentifier] ROWGUIDCOL  NOT NULL,
	[InterfaceID] [uniqueidentifier] NOT NULL,
	[Datetime] [datetime] NOT NULL,
	[MessageContent] [nvarchar](max) NULL,
 CONSTRAINT [PK_messageData] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

ALTER TABLE [dbo].[messageData] ADD  CONSTRAINT [DF_messageData_ID]  DEFAULT (newid()) FOR [ID]
GO

ALTER TABLE [dbo].[messageData]  WITH CHECK ADD  CONSTRAINT [FK_interfaces-ID_messageData-interfaceID] FOREIGN KEY([InterfaceID])
REFERENCES [dbo].[interfaces] ([ID])
GO

ALTER TABLE [dbo].[messageData] CHECK CONSTRAINT [FK_interfaces-ID_messageData-interfaceID]
GO

--Create the KeyValue storage

CREATE TABLE [dbo].[keyValues](
	[MessageID] [uniqueidentifier] NOT NULL,
	[Type] [nvarchar](20) NOT NULL,
	[Value] [nvarchar](255) NULL
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[keyValues]  WITH CHECK ADD  CONSTRAINT [FK_keyValues-MessageID_messageData-ID] FOREIGN KEY([MessageID])
REFERENCES [dbo].[messageData] ([ID])
GO

ALTER TABLE [dbo].[keyValues] CHECK CONSTRAINT [FK_keyValues-MessageID_messageData-ID]
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'List of key values associated with a message to make searching easier.' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'keyValues'
GO

--Create SP purgeMessageData

-- =============================================
-- Author:		Alex Hanson
-- Create date: 2017-09-12
-- Description:	Procedure to remove message data from the database which has been determined to be older than the appropriate time for each interface.
-- =============================================
CREATE PROCEDURE [dbo].[purgeMessageData] 
	@systemPurgeDays TINYINT = 0 --If no number of days set then assume that we should keep messages forever.
AS
BEGIN
	DECLARE @startTime DATETIME = getdate() --Get the current date and time for comparison.
	DECLARE @interface UNIQUEIDENTIFIER
	DECLARE @messageID UNIQUEIDENTIFIER
	DECLARE @intfPurgeDays TINYINT
	DECLARE @purgeDate DATETIME
	DECLARE @count int = 0

	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;

	--Declare & Open the cursor over all interfaces
	DECLARE intfCurs CURSOR FOR
	SELECT ID,PurgeDays FROM interfaces

	OPEN intfCurs
	FETCH NEXT FROM intfCurs into @interface,@intfPurgeDays

	--Loop over all of the interfaces to remove messages.
	WHILE @@FETCH_STATUS = 0 BEGIN
		--First check and see if purge days should be overwritten.
		IF @intfPurgeDays = NULL SET @intfPurgeDays=@systemPurgeDays

		--Now check to see if we should be purging this interface and do so.
		IF @intfPurgeDays <> 0 BEGIN
			SET @purgeDate = DATEADD("D",-@intfPurgeDays,@startTime)

			--Cursor over all of the messages that should be deleted.
			DECLARE msgCurs CURSOR FOR
			SELECT ID FROM messageData WHERE ((InterfaceID = @interface) AND (Datetime < @purgeDate))

			OPEN msgCurs
			FETCH NEXT FROM msgCurs into @messageID

			--Loop over all the messages and remove from the keys and routings table the message before removing the message itself.
			WHILE @@FETCH_STATUS = 0 BEGIN
				
				--Increase our count
				SET @count = @count + 1

				--Delete from the keyValues table.
				DELETE FROM keyValues WHERE MessageID=@messageID
				
				--Delete from the message table.
				DELETE FROM messageData where ID=@messageID 

				--Get the next row.
				FETCH NEXT FROM msgCurs into @messageID
			END

			--Wrap up the use of the message cursor.
			CLOSE msgCurs
			DEALLOCATE msgCurs
		END

		--Fetch the next interface
		FETCH NEXT FROM intfCurs into @interface,@intfPurgeDays
	END

	--Wrap up the use of the interface cursor.
	CLOSE intfCurs
	DEALLOCATE intfCurs

	RETURN @count
END
GO

--Create SP storeMessage

-- =============================================
-- Author:		Alex Hanson
-- Create date: 2017-09-14
-- Description:	Procedure for adding a message to the database.
-- =============================================
CREATE PROCEDURE [dbo].[storeMessage]
	@interface UNIQUEIDENTIFIER, --Specific interface which sent or received the message.
	@messageContent NVARCHAR(MAX), --Full content of the message.
	@dateTime DATETIME,
	@msgID UNIQUEIDENTIFIER OUTPUT
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;

	SET @msgID = NEWID()

    --Insert the new message into the messages table.
	INSERT INTO messageData(ID,InterfaceID,Datetime,MessageContent) VALUES (@msgID,@interface,@dateTime,@messageContent)

END
GO

--Create SP storeKeyValue

-- =============================================
-- Author:		Alex Hanson
-- Create date: 2017-09-14
-- Description:	Creates entries in the keyValues table for the message provided.
-- =============================================
CREATE PROCEDURE [dbo].[storeKeyValue] 
	@messageID UNIQUEIDENTIFIER, --Identifier of the message to be keyed.
	@type NVARCHAR(20), --Message content to be parsed.
	@value NVARCHAR(255) --List of patterns which need to be parsed. These patters are char(0) delimited in the following pattern (ignoring whitespace):
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;

    INSERT INTO keyValues(MessageID,Type,Value) VALUES (@messageID,@type,@value)
END
GO

--Create SP storeHAPIKeyValue

-- =============================================
-- Author:		Alex Hanson
-- Create date: 2017-09-14
-- Description:	Takes a set to fields to pull from the message and stores keys based on those fields.
-- =============================================
CREATE PROCEDURE storeHAPIKeyValue 
	-- Add the parameters for the stored procedure here
	@fieldList NVARCHAR(255), --List of fields which are in the format <SEGMENT>-<FIELD>.<COMPONENT> All repetitions will be stored with the messages repitition seperator between them.
	@msgID UNIQUEIDENTIFIER --Message identifier from which data needs to be pulled for storage.
AS
BEGIN
	
	--Initialize the variables needed.
	DECLARE @content NVARCHAR(MAX) = NULL
	DECLARE @curs CURSOR --Looping cursor.
	DECLARE @field NVARCHAR(10)
	DECLARE @tempSTRA NVARCHAR(4000) --4000 is max string length without using the literal max.
	DECLARE @SEG NCHAR(3)	--Segment
	DECLARE @FLD INT		--Field #
	DECLARE @FS NCHAR(1)	--Field Seperator
	DECLARE @CS NCHAR(1)	--Componenet Seperator
	DECLARE @RS NCHAR(1)	--Repitition Seperator
	DECLARE @indexDash int 
	DECLARE @indexPeriod int 
	DECLARE @tempINTA int
	DECLARE @tempINTB int
	DECLARE @count int
	DECLARE @repitition int
	
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;

	--Get the value of the content from this message
	SELECT @content = MessageContent FROM messageData WHERE ID = @msgID

	--If we didn't get any content returned then there is nothing to do and we should quit.
	IF LEN(@content) < 1 RETURN

	--Determine which seperators this message is using
	SET @tempINTA = CHARINDEX(CHAR(13) + 'MSH', @content)
	SET @FS = SUBSTRING(@content, @tempINTA + 4, 1)
	SET @CS = SUBSTRING(@content, @tempINTA + 5, 1)
	SET @RS = SUBSTRING(@content, @tempINTA + 6, 1)

    --Get the Temp table of the list of fields and then prep the cursor for use.
	SET @curs = CURSOR FOR SELECT * from STRING_SPLIT(@fieldList, ',')
	OPEN @curs
	FETCH NEXT FROM @curs INTO @field

	--Loop over all of the fields in the list.
	WHILE @@FETCH_STATUS = 0 BEGIN

		SET @indexDash = CHARINDEX('-', @field)

		--Seperate the field into it's individual components.
		SET @SEG = SUBSTRING(@field, 1, 3)
		SET @FLD = SUBSTRING(@field, @indexDash + 1, LEN(@field))

		--Get the value from the message by searching and getting the full segment first.
		SET @tempINTA = CHARINDEX(CHAR(13) + @SEG, @content) + 1
		IF @tempINTA = 0 BEGIN
			--Make sure that we get the next entry in the table.
			FETCH NEXT FROM @curs INTO @field

			BREAK  --Quit if there was no segment to be found in the message.
		END
		SET @tempSTRA = SUBSTRING(@content, @tempINTA, CHARINDEX(CHAR(13), @content, @tempINTA + 1) - @tempINTA)

		--Now that we have the segment use the field number to get to that location in the string.
		SET @tempINTA = 1
		IF @SEG = 'MSH' SET @count = 1  --Force MSH to increment one less time to find the field as it is offset of the reset of the segments by one.
		ELSE SET @count = 0
		WHILE @tempINTA <> 0 AND @count < @FLD BEGIN
			SET @tempINTA = CHARINDEX(@FS, @tempSTRA, @tempINTA+1) --If we end up outside the string then tempINTA will be set to 0 by this function.
			SET @count = @count + 1
		END

		--If there was no value to be found then quit.
		IF @tempINTA = 0 BEGIN
			--Make sure that we get the next entry in the table.
			FETCH NEXT FROM @curs INTO @field

			BREAK  --Quit if there was no segment to be found in the message.
		END

		--Get the field Value by getting the end of the field then using that for the string
		SET @tempINTB = CHARINDEX(@FS, @tempSTRA, @tempINTA + 1)
		IF @tempINTB = 0 SET @tempINTB = LEN(@tempSTRA)
		IF @tempINTB - @tempINTA - 1 < 1 SET @tempSTRA = ''
		ELSE SET @tempSTRA = SUBSTRING(@tempSTRA, @tempINTA + 1, @tempINTB - @tempINTA - 1)

		--Store to the key value table.
		INSERT INTO keyValues(MessageID, Type, Value) VALUES (@msgID, @field, @tempSTRA)

		--Make sure that we get the next entry in the table.
		FETCH NEXT FROM @curs INTO @field
	END

	--Close and Deallocate the cursor to prevent bleeding.
	CLOSE @curs
	DEALLOCATE @curs
END
GO

--Create SP storeHAPIMessage

-- =============================================
-- Author:		Alex Hanson
-- Create date: 2017-09-21
-- Description:	Execute both stored procedures for key value and message storage.
-- =============================================
CREATE PROCEDURE [dbo].[storeHAPIMessage] 
	@interface UNIQUEIDENTIFIER, --Specific interface which sent or received the message.
	@messageContent NVARCHAR(MAX), --Full content of the message.
	@fieldList NVARCHAR(255), --List of fields which are in the format <SEGMENT>-<FIELD> All repetitions will be stored with the messages repitition seperator between them.
	@dateTime DATETIME
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
	DECLARE @msgID as UNIQUEIDENTIFIER

    --Insert the message into the database
	EXEC storeMessage @interface, @messageContent, @dateTime, @msgID = @msgID OUTPUT;

	--Execute the Key value mapping.
	EXEC storeHAPIKeyValue @fieldList, @msgID
END
GO

--Insert the expected integration interfaces.
--Scheduling Interface to Vista
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('3E6C7FF1-32DF-4699-8D85-06F59809F956','Scheduling - Epic to Vista','Incoming',NULL) 
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('DCFA837E-DF14-45E0-A440-38B2FDA1985B','Scheduling - Epic to Vista','Outgoing',NULL)

--Scheduling Interface to Epic
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('74B57C05-A581-4E79-8423-FE863D72FE49','Scheduling - Vista to Epic','Incoming',NULL)
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('332A8690-D087-4E8F-871E-0B71A2D44E3F','Scheduling - Vista to Epic','Outgoing',NULL)

--Demographics Interface to Epic
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('58004C37-1846-41DA-B002-26BF92304C7D','Demographics - Vista to Epic','Outgoing',NULL)
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('633C6F74-946C-4C71-B1EA-2129E5B37952','Demographics - Vista to Epic','Incoming',NULL)

--PCMM Interface to Epic
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('FDD64944-665D-4AAD-9C8A-BAADC496AF2C','PCMM - PCMM to Epic','Outgoing',NULL)
--No incoming interface as these messages are being built by the adapter processes from data provided by Ensemble and PCMM.

--Orders
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('4C92FB81-769E-44E4-B0FE-9AEAB04A84DD','Orders - Vista to Epic','Incoming',NULL)
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('FEC46370-0CB1-436B-BFF1-2898E5C4F8D6','Orders - Vista to Epic','Outgoing',NULL)

--Errors Interface
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('C95808C1-8597-43E4-83DD-757CDF283574','Errors - Vista to Epic','Incoming',NULL)
Insert into interfaces(ID,Name,Direction,PurgeDays) values ('8FA5C77E-DAB1-46F2-8642-14D69415AF89','Errors - Vista to Epic','Outgoing',NULL)
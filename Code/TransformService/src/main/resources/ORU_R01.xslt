<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:v="urn:hl7-org:v2xml">

  <xsl:variable name="v">
    <xsl:value-of select="namespace-uri(*)"/>
  </xsl:variable>

  <!--identity recursion that copies ENTIRE xml
      except where other templates below match-->
  <xsl:template match="/|node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!--  Needed to add ZFY at end of XML-->
  <xsl:template match="v:ORU_R01">
    <xsl:element name="ORU_R01" namespace="{$v}">
      <xsl:apply-templates select="node()"/>
      <xsl:call-template name="addZFY"/>
    </xsl:element>
  </xsl:template>

  <!--  Template to construct brand new ZFY segments-->
  <xsl:template name="addZFY">
    <!-- Every patient observation becomes a ZFY segment-->
    <xsl:for-each select="/v:ORU_R01/v:ORU_R01.PATIENT_RESULT[1]/v:ORU_R01.ORDER_OBSERVATION">
      <xsl:element name="ORU_R01" namespace="{$v}"> </xsl:element>

      <xsl:element name="ZFY" namespace="{$v}">
        <xsl:element name="ZFY.1" namespace="{$v}"> VA FLAG </xsl:element>
        <xsl:element name="ZFY.2" namespace="{$v}">
          <xsl:value-of select="v:ORU_R01.OBSERVATION[v:OBX/v:OBX.2 = 'ST']/v:OBX/v:OBX.5/."/>
        </xsl:element>
        <xsl:element name="ZFY.3" namespace="{$v}">
          <xsl:value-of select="v:OBR[1]/v:OBR.4[1]/v:CE.1[1]/."/>
        </xsl:element>

        <!-- loop every narrative (TX) -->
        <xsl:for-each select="v:ORU_R01.OBSERVATION">
          <xsl:if test="v:OBX/v:OBX.2/. = 'TX'">
            <xsl:element name="ZFY.4" namespace="{$v}">
              <xsl:value-of select="v:OBX/v:OBX.5/."/>
            </xsl:element>
          </xsl:if>
        </xsl:for-each>

      </xsl:element>
    </xsl:for-each>
  </xsl:template>

  <!--  swap Message Type and Event from ORU^R01 to ADT^A31-->
  <xsl:template match="v:MSH/v:MSH.9">
    <xsl:element name="MSH.9" namespace="{$v}">
      <xsl:element name="MSG.1" namespace="{$v}"> ADT </xsl:element>
      <xsl:element name="MSG.2" namespace="{$v}"> A31 </xsl:element>
    </xsl:element>
  </xsl:template>

  <!--  delete all Observations-->
  <xsl:template match="v:ORU_R01.ORDER_OBSERVATION"/>

</xsl:stylesheet>

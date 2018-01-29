<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:v="urn:hl7-org:v2xml" version="1.0">
  <xsl:output omit-xml-declaration="yes" indent="yes"/>
  <xsl:strip-space elements="*"/>
  <!-- store namespace in variable for readability -->
  <xsl:variable name="v">urn:hl7-org:v2xml</xsl:variable>
  <!-- identity template -->
  <xsl:template match="/|node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>
  <!-- root node -->
  <xsl:template match="v:ORU_R01">
    <xsl:element name="ADT_A31" namespace="{$v}">
      <xsl:apply-templates select="node()"/>
    </xsl:element>
  </xsl:template>
  <!-- swap Message Type and Event from ORU^R01 to ADT^A31 -->
  <xsl:template match="v:MSH/v:MSH.9">
    <xsl:element name="MSH.9" namespace="{$v}">
      <xsl:element name="C.1" namespace="{$v}">ADT</xsl:element>
      <xsl:element name="C.2" namespace="{$v}">A31</xsl:element>
    </xsl:element>
  </xsl:template>
  <!-- Give all the OBXs unique keys based on the previous OBR -->
  <xsl:key name="kOBX" match="v:OBX" use="generate-id(preceding-sibling::v:OBR[1])"/>
  <!-- Turn OBRs into ZFYs -->
  <xsl:template match="v:OBR">
    <xsl:element name="ZFY" namespace="{$v}">
      <!-- ZFY-1 = OBR-4 -->
      <xsl:element name="ZFY.1" namespace="{$v}">
		<xsl:copy-of select="v:OBR.4/*"/>
	  </xsl:element>
      <!-- ZFY-2 = OBX-5 for the OBX with OBX-3 = "Status" -->
        <xsl:for-each select="key('kOBX', generate-id(current()))">
          <xsl:if test="v:OBX.3 = 'Status'">
            <xsl:element name="ZFY.2" namespace="{$v}">
              <xsl:copy-of select="v:OBX.5/*"/>
            </xsl:element>
          </xsl:if>
        </xsl:for-each>
      <!-- ZFY-3 = OBR-4 -->
      <xsl:element name="ZFY.3" namespace="{$v}">
        <xsl:copy-of select="v:OBR.4/*"/>
      </xsl:element>
      <!-- ZFY-4 = OBX-5 of every OBX without OBX = "Status" -->
      <xsl:element name="ZFY.4" namespace="{$v}">
        <xsl:element name="C.1" namespace="{$v}">
          <xsl:for-each select="key('kOBX', generate-id(current()))">
            <xsl:if test="v:OBX.3 != 'Status'">
              <xsl:for-each select="v:OBX.5/v:C.1">
                <xsl:value-of select="."/><xsl:text>\.br\</xsl:text>
              </xsl:for-each>
            </xsl:if>
          </xsl:for-each>
        </xsl:element>
      </xsl:element>
    </xsl:element>
  </xsl:template>
  <!-- and finally remove the OBXs-->
  <xsl:template match="v:OBX"/>
</xsl:stylesheet>

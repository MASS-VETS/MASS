<?xml version="1.0" encoding="UTF-8"?>

<!--  Handles the following-->
<!--  A08, A28, A40 -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:v="urn:hl7-org:v2xml">
  <xsl:output omit-xml-declaration="yes" indent="yes"/>
  <xsl:strip-space elements="*"/>

  <xsl:variable name="v">urn:hl7-org:v2xml</xsl:variable>

  <!--identity recursion that copies ENTIRE xml
      except where other templates below match-->
  <xsl:template match="/|node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>
  
  <!--  Needed to add NK1,ZFY,ZEL at end of XML-->
  <xsl:template match="v:ADT_A08">
    <xsl:element name="ADT_A08" namespace="{$v}">
      <xsl:call-template name="topLevel"/>
    </xsl:element>  
  </xsl:template>
  
  <!--  Needed to add NK1,ZFY,ZEL at end of XML-->
  <xsl:template match="v:ADT_A28">
    <xsl:element name="ADT_A28" namespace="{$v}">
      <xsl:call-template name="topLevel"/>
    </xsl:element>  
  </xsl:template>
  
  <xsl:template name="topLevel">
    <xsl:apply-templates select="node()"/>
    <xsl:call-template name="addNK1"/>
    <xsl:call-template name="addZFY"/>
    <xsl:call-template name="addZEL"/>
  </xsl:template>
  
  <!--  rename entire ZCT segment to NK1-->
  <xsl:template name="addNK1">
    <xsl:element name="NK1" namespace="{$v}">
      <xsl:element name="NK1.1" namespace="{$v}">
        <xsl:copy-of select="v:ZCT/v:ZCT.1/*" />
      </xsl:element>
      <xsl:element name="NK1.2" namespace="{$v}">
        <xsl:copy-of select="v:ZCT/v:ZCT.3/*" />
      </xsl:element>
      <xsl:element name="NK1.3" namespace="{$v}">
        <xsl:copy-of select="v:ZCT/v:ZCT.4/*" />
      </xsl:element>
      <xsl:element name="NK1.4" namespace="{$v}">
        <xsl:copy-of select="v:ZCT/v:ZCT.5/*" />
      </xsl:element>
      <xsl:element name="NK1.5" namespace="{$v}">
        <xsl:copy-of select="v:ZCT/v:ZCT.6/*" />
      </xsl:element>
    </xsl:element>
  </xsl:template>
  
  <!--  Template to construct brand new ZFY segments-->
  <xsl:template name="addZFY">
    <xsl:element name="ZFY" namespace="{$v}">
      <xsl:element name="ZFY.1" namespace="{$v}">
        <xsl:element name="C.1" namespace="{$v}">SERVICE CONNECTED</xsl:element>    
      </xsl:element>
      <xsl:choose>
        <xsl:when test="v:ZSP/v:ZSP.2/. = 'Y'">
          <xsl:element name="ZFY.2" namespace="{$v}">
            <xsl:element name="C.1" namespace="{$v}">ACTIVE</xsl:element>  
          </xsl:element>
          <xsl:element name="ZFY.3" namespace="{$v}">
            <xsl:element name="C.1" namespace="{$v}">SERVICE CONNECTED</xsl:element>
          </xsl:element>
          <xsl:element name="ZFY.4" namespace="{$v}">
            <xsl:element name="C.1" namespace="{$v}"><xsl:value-of select="v:ZSP/v:ZSP.3"/>% Service Connected</xsl:element>
          </xsl:element>
        </xsl:when>
        <xsl:otherwise>
          <xsl:element name="ZFY.2" namespace="{$v}"> 
            <xsl:element name="C.1" namespace="{$v}">
              INACTIVE
            </xsl:element>              
          </xsl:element>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>
  
  <!--  smoosh multiple ZEL lines into 1 ZEL-->
  <xsl:template name="addZEL">
    <xsl:element name="ZEL" namespace="{$v}">
      <xsl:for-each select="v:ZEL">
        <xsl:element name="ZEL.1" namespace="{$v}">
          <xsl:element name="C.1" namespace="{$v}">
          <xsl:value-of select="v:ZEL.2/."/>
        </xsl:element>
        </xsl:element>
      </xsl:for-each>
      <xsl:element name="ZEL.2" namespace="{$v}">
        <xsl:element name="C.1" namespace="{$v}">
        <xsl:value-of select="v:ZSP/v:ZSP.12"/>
        </xsl:element>
      </xsl:element>
      <xsl:element name="ZEL.3" namespace="{$v}">
        <xsl:element name="C.1" namespace="{$v}">
        <xsl:value-of select="v:ZEN/v:ZEN.9"/>
        <xsl:value-of select="v:ZEN/v:ZEN.13"/>
        </xsl:element>
      </xsl:element>
      <xsl:element name="ZEL.4" namespace="{$v}">
        <xsl:element name="C.1" namespace="{$v}">
        <xsl:value-of select="v:ZEN/v:ZEN.4"/>
        </xsl:element>
      </xsl:element>
      <xsl:element name="ZEL.5" namespace="{$v}">
        <xsl:element name="C.1" namespace="{$v}">
        <xsl:value-of select="v:ZEL[1]/v:ZEL.37"/>
        </xsl:element>
      </xsl:element>
      <xsl:element name="ZEL.6" namespace="{$v}">
        <xsl:element name="C.1" namespace="{$v}">
        <xsl:value-of select="v:ZEL[1]/v:ZEL.38"/>
        </xsl:element>
      </xsl:element>
    </xsl:element>
  </xsl:template>
  
  <!--  swap Message Type and Event from ADT^A08 to ADT^A31-->
  <!--  but DO NOT swap Message Type ADT^A28 to ADT^A31-->
  <xsl:template match="v:ADT_A08/v:MSH/v:MSH.9">
    <xsl:element name="MSH.9" namespace="{$v}">
      <xsl:element name="C.1" namespace="{$v}">ADT</xsl:element>
      <xsl:element name="C.2" namespace="{$v}">A31</xsl:element>
    </xsl:element>
  </xsl:template>
  
  <!--  delete all ZCT-->
  <xsl:template match="v:ADT_A08/v:ZCT"/>
  <xsl:template match="v:ADT_A28/v:ZCT"/>
  
  <!--  delete all ZEL-->
  <xsl:template match="v:ADT_A08/v:ZEL"/>
  <xsl:template match="v:ADT_A28/v:ZEL"/>
  
  <!--  delete all ZEN-->
  <xsl:template match="v:ADT_A08/v:ZEN"/>
  <xsl:template match="v:ADT_A28/v:ZEN"/>
  
  <!--  delete all ZSP-->
  <xsl:template match="v:ADT_A08/v:ZSP"/>
  <xsl:template match="v:ADT_A28/v:ZSP"/>
  
</xsl:stylesheet>
package gov.va.mass.adapter.core.hl7v2;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MSHTests {
	
	@Test
	public void testMSHEmpty() {
		MSH msh = new MSH("");
		assertEquals("|", msh.fs);
		assertEquals("", msh.get(0));
		assertEquals("|", msh.get(1));
		assertEquals("", msh.get(2));
		assertEquals("", msh.get(20));
	}
	
	@Test
	public void testMSH() {
		MSH msh = new MSH("MSH");
		assertEquals("|", msh.fs);
		assertEquals("", msh.get(0));
		assertEquals("|", msh.get(1));
		assertEquals("", msh.get(2));
		assertEquals("", msh.get(20));
	}
	
	@Test
	public void testMSHpipe() {
		MSH msh = new MSH("MSH|");
		assertEquals("|", msh.fs);
		assertEquals("", msh.get(0));
		assertEquals("|", msh.get(1));
		assertEquals("", msh.get(2));
	}
	
	@Test
	public void testMSHstar() {
		MSH msh = new MSH("MSH*");
		assertEquals("*", msh.fs);
		assertEquals("*", msh.get(1));
	}
	
	@Test
	public void testMSH2() {
		MSH msh = new MSH("MSH|^~\\&");
		assertEquals("^~\\&", msh.get(2));
	}
	
	@Test
	public void testMSHcontrolDefault() {
		MSH msh = new MSH("MSH|^~\\&");
		assertEquals("|", msh.fs);
		assertEquals("^", msh.cs);
		assertEquals("~", msh.rs);
		assertEquals("\\", msh.es);
		assertEquals("&", msh.ss);
	}
	
	@Test
	public void testMSHcontrolWeird1() {
		MSH msh = new MSH("MSH!@#$%!");
		assertEquals("!", msh.fs);
		assertEquals("@", msh.cs);
		assertEquals("#", msh.rs);
		assertEquals("$", msh.es);
		assertEquals("%", msh.ss);
	}
	
	@Test
	public void testMSHcontrolWeird2() {
		MSH msh = new MSH("MSH!@!");
		assertEquals("!", msh.fs);
		assertEquals("@", msh.cs);
		assertEquals("~", msh.rs);
		assertEquals("\\", msh.es);
		assertEquals("&", msh.ss);
	}
	
	@Test
	public void testMSH6() {
		MSH msh = new MSH("MSH|^~\\&|SendingApplication|SendingFacility|ReceivingApplication|ReceivingFacility");
		assertEquals("SendingApplication", msh.get(3));
		assertEquals("SendingFacility", msh.get(4));
		assertEquals("ReceivingApplication", msh.get(5));
		assertEquals("ReceivingFacility", msh.get(6));
		assertEquals("MSH|^~\\&|SendingApplication|SendingFacility|ReceivingApplication|ReceivingFacility", msh.toString());
	}
	
	@Test
	public void testNewMSH6() {
		MSH msh0 = new MSH("MSH|^~\\&|SendingApplication|SendingFacility|ReceivingApplication|ReceivingFacility");
		MSH msh = new MSH(msh0);
		assertEquals("|", msh.fs);
		assertEquals("", msh.get(0));
		assertEquals("|", msh.get(1));
		assertEquals("SendingApplication", msh.get(3));
		assertEquals("SendingFacility", msh.get(4));
		assertEquals("ReceivingApplication", msh.get(5));
		assertEquals("ReceivingFacility", msh.get(6));
		assertEquals("MSH|^~\\&|SendingApplication|SendingFacility|ReceivingApplication|ReceivingFacility", msh.toString());
	}
	
	@Test
	public void testMshSet20() {
		MSH msh = new MSH("MSH|^~\\&");
		msh.set(20, "banana");
		assertEquals("banana", msh.get(20));
		assertEquals("MSH|^~\\&||||||||||||||||||banana", msh.toString());
	}
}

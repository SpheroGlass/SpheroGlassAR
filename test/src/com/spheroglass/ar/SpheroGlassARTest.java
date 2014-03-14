package com.spheroglass.ar;

import junit.framework.TestCase;

public class SpheroGlassARTest extends TestCase {

	private SpheroGlassAR spheroGlassAR;

	protected void setUp() throws Exception {
		super.setUp();
		spheroGlassAR = new SpheroGlassAR();
	}

	public void testSetPointIsUp() {
		spheroGlassAR.setPoint(0, 100);
		assertTrue("speed>0", spheroGlassAR.getSpeed()>0);
		assertEquals("direction when point up", 180, spheroGlassAR.getDirection());
	}

	public void testSetPointDown() {
		spheroGlassAR.setPoint(0, -100);
		assertTrue("speed>0", spheroGlassAR.getSpeed()>0);
		assertEquals("direction when point down", 0, spheroGlassAR.getDirection());
	}

	public void testSetPointIsLeft() {
		spheroGlassAR.setPoint(-100, 0);
		assertTrue("speed>0", spheroGlassAR.getSpeed()>0);
		assertEquals("direction when point left", 90, spheroGlassAR.getDirection());
	}

	public void testSetPointIsRight() {
		spheroGlassAR.setPoint(100, 0);
		assertTrue("speed>0", spheroGlassAR.getSpeed()>0);
		assertEquals("direction when point right", 270, spheroGlassAR.getDirection());
	}
}

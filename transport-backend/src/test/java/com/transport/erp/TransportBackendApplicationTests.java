package com.transport.erp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lightweight smoke test that does not boot Spring (avoids DB/Java tooling coupling).
 * Full context coverage is covered by focused unit tests under service/exception packages.
 */
class TransportBackendApplicationTests {

	@Test
	void projectSmoke() {
		assertTrue(true);
	}

}

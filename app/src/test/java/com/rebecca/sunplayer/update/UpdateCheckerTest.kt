package com.rebecca.sunplayer.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `newer version is detected when patch component increases`() {
        assertTrue(UpdateChecker.isNewerVersion("1.3.1", "1.3.0"))
    }

    @Test
    fun `four part server version compares against three part installed version`() {
        assertTrue(UpdateChecker.isNewerVersion("1.4.0", "1.3.0.1"))
    }

    @Test
    fun `installed version is not newer than itself`() {
        assertFalse(UpdateChecker.isNewerVersion("1.3.0", "1.3.0"))
    }

    @Test
    fun `older release is rejected`() {
        assertFalse(UpdateChecker.isNewerVersion("1.3.0.1", "1.4.0"))
    }

    @Test
    fun `major version bump wins`() {
        assertTrue(UpdateChecker.isNewerVersion("2.0", "1.9.9"))
    }

    @Test
    fun `multi digit components compare numerically not lexically`() {
        assertTrue(UpdateChecker.isNewerVersion("1.2.10", "1.2.9"))
    }

    @Test
    fun `missing trailing components count as zero`() {
        // "1.4" == "1.4.0.0", which is OLDER than "1.4.0.1".
        assertFalse(UpdateChecker.isNewerVersion("1.4", "1.4.0.1"))
        // The reverse direction: "1.4.0.1" is newer than "1.4".
        assertTrue(UpdateChecker.isNewerVersion("1.4.0.1", "1.4"))
        // Equal once padded: neither is newer.
        assertFalse(UpdateChecker.isNewerVersion("1.4", "1.4.0"))
        assertFalse(UpdateChecker.isNewerVersion("1.4.0", "1.4"))
    }

    @Test
    fun `non numeric version yields no update`() {
        assertFalse(UpdateChecker.isNewerVersion("nightly", "1.4.0"))
    }
}
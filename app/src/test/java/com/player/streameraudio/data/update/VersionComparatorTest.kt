package com.player.streameraudio.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test fun newerMinorVersionIsDetected() {
        assertTrue(VersionComparator.isNewer("2.2.0", "2.1.0"))
    }

    @Test fun tagPrefixIsIgnored() {
        assertTrue(VersionComparator.isNewer("v3.0.0", "2.9.9"))
    }

    @Test fun equalVersionIsNotNewer() {
        assertFalse(VersionComparator.isNewer("2.1.0", "2.1.0-debug"))
    }

    @Test fun olderVersionIsNotNewer() {
        assertFalse(VersionComparator.isNewer("1.9.9", "2.0.0"))
    }
}

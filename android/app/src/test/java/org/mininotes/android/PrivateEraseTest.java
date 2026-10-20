package org.mininotes.android;

import static org.junit.Assert.*;
import org.junit.Test;

/** erase//: in a private note (decision 201): found in any case, and nothing else is. */
public class PrivateEraseTest {
    @Test public void theCodeIsFoundWhereItIsAndNowhereElse() {
        assertArrayEquals(new int[]{6,14},PrivateCode.erase("Hello erase//: there"));
        assertArrayEquals(new int[]{0,8},PrivateCode.erase("ERASE//:"));
        assertNull(PrivateCode.erase("erase this line"));
        assertNull(PrivateCode.erase("erase//"));
        assertNull(PrivateCode.erase(null));
    }
}

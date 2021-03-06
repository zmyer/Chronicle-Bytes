package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CASTest {
    @Test
    public void testCAS() {
        HexDumpBytes bytes = new HexDumpBytes();

        bytes.comment("s32").writeUtf8("s32");
        long s32 = bytes.writePosition();
        bytes.writeInt(0);

        bytes.comment("s64").writeUtf8("s64");
        long s64 = bytes.writePosition();
        bytes.writeLong(0);

        System.out.println(bytes.toHexString());

        assertTrue(bytes.compareAndSwapInt(s32, 0, Integer.MAX_VALUE));
        assertTrue(bytes.compareAndSwapLong(s64, 0, Long.MAX_VALUE));

        System.out.println(bytes.toHexString());

        assertEquals("03 73 33 32 ff ff ff 7f                         # s32\n" +
                        "03 73 36 34 ff ff ff ff ff ff ff 7f             # s64\n",
                bytes.toHexString());
    }
}

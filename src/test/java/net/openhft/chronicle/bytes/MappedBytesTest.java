package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import static org.junit.Assert.*;

public class MappedBytesTest {

    final private String
            smallText = "It's ten years since the iPhone was first unveiled and Apple has marked " +
            "the occas" +
            "ion with a new iPhone that doesn't just jump one generation, it jumps several. " +
            "Apple has leapt straight from iPhone 7 (via the iPhone 8, reviewed here) all the way " +
            "to iPhone 10 (yes, that's how you are supposed to say it).\n" +
            "\n" +
            "Read on to find out how the new flagship iPhone X shapes up. Is it going to revolutionise " +
            "the mobile phone again like the original iPhone did, or is Apple now just playing catch-up " +
            "with the rest of the industry? (For a comparison with one rival device, see iPhone X vs LG G7.)\n";

    private final StringBuilder largeTextBuilder = new StringBuilder();

    private String text;

    {
        for (int i = 0; i < 200; i++) {
            largeTextBuilder.append(smallText);
        }

        text = largeTextBuilder.toString();
    }

    @Test
    public void testWriteBytes() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 4, 4);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 200 << 10, 200 << 10);) {

            // write
            bytesW.write(Bytes.from(text));
            long wp = bytesW.writePosition;
            Assert.assertEquals(text.length(), bytesW.writePosition);

            // read
            bytesR.readLimit(wp);

            Assert.assertEquals(text, bytesR.toString());
        }

    }

    @Test
    public void testWriteReadBytes() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);) {

            // write
            bytesW.write(Bytes.from(text));
            long wp = bytesW.writePosition;
            Assert.assertEquals(text.length(), bytesW.writePosition);

            // read
            bytesR.readLimit(wp);

            Assert.assertEquals(text, bytesR.toString());
        }

    }


    @Test
    public void testWriteBytesWithOffset() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 4, 4);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 200 << 10, 200 << 10);) {

            int offset = 10;

            // write
            bytesW.write(offset, Bytes.from(text));
            long wp = text.length() + offset;
            Assert.assertEquals(0, bytesW.writePosition);

            // read
            bytesR.readLimit(wp);
            bytesR.readPosition(offset);
            Assert.assertEquals(text, bytesR.toString());
        }
    }

    @Test
    public void testWriteReadBytesWithOffset() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);) {

            int offset = 10;

            // write
            bytesW.write(offset, Bytes.from(text));
            long wp = text.length() + offset;
            Assert.assertEquals(0, bytesW.writePosition);

            // read
            bytesR.readLimit(wp);
            bytesR.readPosition(offset);
            Assert.assertEquals(text, bytesR.toString());
        }
    }


    @Test
    public void testWriteBytesWithOffsetAndTextShift() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 4, 4);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 200 << 10, 200 << 10);) {
            int offset = 10;
            int shift = 128;

            //write
            bytesW.write(offset, Bytes.from(text), shift, text.length() - shift);
            Assert.assertEquals(0, bytesW.writePosition);

            // read
            bytesR.readLimit(offset + (text.length() - shift));
            bytesR.readPosition(offset);
            String actual = bytesR.toString();
            Assert.assertEquals(text.substring(shift), actual);
        }
    }

    @Test
    public void testWriteReadBytesWithOffsetAndTextShift() throws IOException {
        File tempFile1 = File.createTempFile("mapped", "bytes");
        try (MappedBytes bytesW = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);
             MappedBytes bytesR = MappedBytes.mappedBytes(tempFile1, 64 << 10, 16 << 10);) {
            int offset = 10;
            int shift = 128;

            //write
            bytesW.write(offset, Bytes.from(text), shift, text.length() - shift);
            Assert.assertEquals(0, bytesW.writePosition);

            // read
            bytesR.readLimit(offset + (text.length() - shift));
            bytesR.readPosition(offset);
            String actual = bytesR.toString();
            Assert.assertEquals(text.substring(shift), actual);
        }
    }


    @Test
    public void testLargeWrites() throws IOException {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 128 <<
                10, 64 << 10);

        byte[] largeBytes = new byte[500 << 10];
        bytes.writePosition(0);
        bytes.write(largeBytes);
        bytes.writePosition(0);
        bytes.write(64, largeBytes);
        bytes.writePosition(0);
        bytes.write(largeBytes, 64, largeBytes.length - 64);
        bytes.writePosition(0);
        bytes.write(64, largeBytes, 64, largeBytes.length - 64);

        bytes.writePosition(0);
        bytes.write(Bytes.wrapForRead(largeBytes));
        bytes.writePosition(0);
        Bytes<byte[]> bytes1 = Bytes.wrapForRead(largeBytes);
        bytes.write(64, bytes1);
        bytes.writePosition(0);
        bytes.write(Bytes.wrapForRead(largeBytes), 64L, largeBytes.length - 64L);
        bytes.writePosition(0);
        bytes.write(64, Bytes.wrapForRead(largeBytes), 64L, largeBytes.length - 64L);

        Bytes bytes2 = Bytes.allocateDirect(largeBytes);
        bytes.writePosition(0);
        bytes.write(bytes2);
        bytes.writePosition(0);
        bytes.write(64, bytes2);
        bytes.writePosition(0);
        bytes.write(bytes2, 64L, largeBytes.length - 64L);
        bytes.writePosition(0);
        bytes.write(64, bytes2, 64L, largeBytes.length - 64L);

        bytes2.release();
        bytes.release();

    }

    @Test
    public void testLargeWrites3() throws IOException {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 47 <<
                10, 21 << 10);

        byte[] largeBytes = new byte[513 << 10];
        bytes.writePosition(0);
        bytes.write(largeBytes);
        bytes.writePosition(0);
        bytes.write(64, largeBytes);
        bytes.writePosition(0);
        bytes.write(largeBytes, 64, largeBytes.length - 64);
        bytes.writePosition(0);
        bytes.write(64, largeBytes, 64, largeBytes.length - 64);

        bytes.writePosition(0);
        bytes.write(Bytes.wrapForRead(largeBytes));
        bytes.writePosition(0);
        Bytes<byte[]> bytes1 = Bytes.wrapForRead(largeBytes);
        bytes.write(64, bytes1);
        bytes.writePosition(0);
        bytes.write(Bytes.wrapForRead(largeBytes), 64L, largeBytes.length - 64L);
        bytes.writePosition(0);
        bytes.write(64, Bytes.wrapForRead(largeBytes), 64L, largeBytes.length - 64L);

        Bytes bytes2 = Bytes.allocateDirect(largeBytes);
        bytes.writePosition(0);
        bytes.write(bytes2);
        bytes.writePosition(0);
        bytes.write(64, bytes2);
        bytes.writePosition(0);
        bytes.write(bytes2, 64L, largeBytes.length - 64L);
        bytes.writePosition(0);
        bytes.write(64, bytes2, 64L, largeBytes.length - 64L);

        bytes2.release();
        bytes.release();

    }

    @Test
    public void testLargeWrites2() throws IOException {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 128 <<
                10, 128 << 10);

        byte[] largeBytes = new byte[500 << 10];
        bytes.writePosition(0);
        bytes.write(largeBytes);
        bytes.writePosition(0);
        bytes.write(64, largeBytes);
        bytes.writePosition(0);
        bytes.write(largeBytes, 64, largeBytes.length - 64);
        bytes.writePosition(0);
        bytes.write(64, largeBytes, 64, largeBytes.length - 64);

        bytes.writePosition(0);
        bytes.write(Bytes.wrapForRead(largeBytes));
        bytes.writePosition(0);
        Bytes<byte[]> bytes1 = Bytes.wrapForRead(largeBytes);
        bytes.write(64, bytes1);
        bytes.writePosition(0);
        bytes.write(Bytes.wrapForRead(largeBytes), 64L, largeBytes.length - 64L);
        bytes.writePosition(0);
        bytes.write(64, Bytes.wrapForRead(largeBytes), 64L, largeBytes.length - 64L);

        Bytes bytes2 = Bytes.allocateDirect(largeBytes);
        bytes.writePosition(0);
        bytes.write(bytes2);
        bytes.writePosition(0);
        bytes.write(64, bytes2);
        bytes.writePosition(0);
        bytes.write(bytes2, 64L, largeBytes.length - 64L);
        bytes.writePosition(0);
        bytes.write(64, bytes2, 64L, largeBytes.length - 64L);

        bytes2.release();
        bytes.release();

    }

    @Test
    public void shouldNotBeReadOnly() throws Exception {
        MappedBytes bytes = MappedBytes.mappedBytes(File.createTempFile("mapped", "bytes"), 64 << 10);
        assertFalse(bytes.isBackingFileReadOnly());
        bytes.writeUtf8(null); // used to blow up.
        assertNull(bytes.readUtf8());
        bytes.release();
    }

    @Test
    public void shouldBeReadOnly() throws Exception {
        final File tempFile = File.createTempFile("mapped", "bytes");
        final RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        raf.setLength(4096);
        assertTrue(tempFile.setWritable(false));
        final MappedBytes mappedBytes = MappedBytes.readOnly(tempFile);

        assertTrue(mappedBytes.
                isBackingFileReadOnly());
        mappedBytes.release();
    }

    @Test(expected = IllegalStateException.class)
    public void interrupted() throws FileNotFoundException {
        Thread.currentThread().interrupt();
        File file = new File(OS.TARGET + "/interrupted-" + System.nanoTime());
        file.deleteOnExit();
        MappedBytes mb = MappedBytes.mappedBytes(file, 64 << 10);
        try {
            mb.realCapacity();
        } finally {
            mb.release();
        }
    }

    @After
    public void clearInterrupt() {
        Thread.interrupted();
    }
}
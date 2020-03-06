package com.trxs.commons.convert;

public class BytesTools
{
    private static final int BIG_ENDIAN = 0;
    private static final int LITTLE_ENDIAN = 1;

    private static int byteOrder = 0;

    public static void integerToBytes( int integer, byte []buffer, int offset )
    {
        if ( byteOrder == BIG_ENDIAN )
        {
            buffer[offset++] = (byte) ((0xff000000 & integer) >> 24);
            buffer[offset++] = (byte) ((0xff0000 & integer) >> 16);
            buffer[offset++] = (byte) ((0xff00 & integer) >> 8);
            buffer[offset++] = (byte) (0xff & integer);
        }
        else
        {
            buffer[offset++] = (byte) (0xff & integer);
            buffer[offset++] = (byte) ((0xff00 & integer) >> 8);
            buffer[offset++] = (byte) ((0xff0000 & integer) >> 16);
            buffer[offset++] = (byte) ((0xff000000 & integer) >> 24);
        }
    }

    public static int bytesToInteger(byte[] src, int offset)
    {
        if ( byteOrder == LITTLE_ENDIAN )
            return (src[offset] & 0xFF) | ((src[offset + 1] & 0xFF) << 8) | ((src[offset + 2] & 0xFF) << 16) | ((src[offset + 3] & 0xFF) << 24);
        else
            return (src[offset] & 0xFF) << 24 | ((src[offset + 1] & 0xFF) << 16) | ((src[offset + 2] & 0xFF) << 8) | (src[offset + 3] & 0xFF);
    }

    public static void longToBytes( long value, byte []buffer, int offset )
    {
        if ( byteOrder == BIG_ENDIAN )
        {
            buffer[offset++] = (byte) ((0xff00000000000000l & value) >> 56);
            buffer[offset++] = (byte) ((0xff000000000000l & value) >> 48);
            buffer[offset++] = (byte) ((0xff0000000000l & value) >> 40);
            buffer[offset++] = (byte) ((0xff00000000l & value) >> 32);
            buffer[offset++] = (byte) ((0xff000000 & value) >> 24);
            buffer[offset++] = (byte) ((0xff0000 & value) >> 16);
            buffer[offset++] = (byte) ((0xff00 & value) >> 8);
            buffer[offset++] = (byte) ( 0xff & value);
        }
        else
        {
            buffer[offset++] = (byte) ( 0xff & value);
            buffer[offset++] = (byte) ((0xff00 & value) >> 8);
            buffer[offset++] = (byte) ((0xff0000 & value) >> 16);
            buffer[offset++] = (byte) ((0xff000000 & value) >> 24);
            buffer[offset++] = (byte) ((0xff00000000l & value) >> 32);
            buffer[offset++] = (byte) ((0xff0000000000l & value) >> 40);
            buffer[offset++] = (byte) ((0xff000000000000l & value) >> 48);
            buffer[offset++] = (byte) ((0xff00000000000000l & value) >> 56);
        }
    }

    public static long bytesToLong(byte[] src, int offset)
    {
        if ( byteOrder == LITTLE_ENDIAN )
            return  (src[offset + 0] & 0xFF) |
                    (src[offset + 1] & 0xFF) <<  8 |
                    (src[offset + 2] & 0xFF) << 16 |
                    (src[offset + 3] & 0xFF) << 24 |
                    (src[offset + 4] & 0xFF) << 32 |
                    (src[offset + 5] & 0xFF) << 40 |
                    (src[offset + 6] & 0xFF) << 48 |
                    (src[offset + 7] & 0xFF) << 56;
        else
            return
                    (src[offset + 0] & 0xFF) << 56 |
                    (src[offset + 1] & 0xFF) << 48 |
                    (src[offset + 2] & 0xFF) << 40 |
                    (src[offset + 3] & 0xFF) << 32 |
                    (src[offset + 4] & 0xFF) << 24 |
                    (src[offset + 5] & 0xFF) << 16 |
                    (src[offset + 6] & 0xFF) <<  8 |
                    (src[offset + 7] & 0xFF);
    }
}

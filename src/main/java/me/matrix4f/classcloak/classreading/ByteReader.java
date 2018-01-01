package me.matrix4f.classcloak.classreading;

public class ByteReader {

    private byte[] data;
    private int index;

    public ByteReader(byte[] in) {
        this.data = in;
    }

    public short u2() {
        int a = data[index] & 0xff;
        int a2 = data[index+1] & 0xff;
        index += 2;
        return (short) ((a << 8) + (a2 << 0));
    }

    public int u4() {
        int a = data[index] & 0xff,
             a2 = data[index+1] & 0xff,
             a3 = data[index+2] & 0xff,
             a4 = data[index+3] & 0xff;
        index += 4;
        return (a << 24) + (a2 << 16) + (a3 << 8) + (a4 << 0);
    }

    public byte u1() {
        return (byte) (data[index++] & 0xff);
    }

    public void skip(int bytes) {
        index += bytes;
    }

    public void reset() {
        index = 0;
    }
}

package server;

import java.nio.ByteBuffer;

public class ByteBufferTool {
    public static byte[] getBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
    public static ByteBuffer getByteBuffer(byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }
    public static ByteBuffer getByteBuffer(byte[] bytes, int offset, int length) {
        return ByteBuffer.wrap(bytes, offset, length);
    }
    public static ByteBuffer getByteBuffer(ByteBuffer buffer, int length) {
        return ByteBuffer.wrap(getBytes(buffer), 0, length);
    }
    public static ByteBuffer getByteBuffer(ByteBuffer buffer) {
        return ByteBuffer.wrap(getBytes(buffer));
    }
    public static byte[] splitBytes(ByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    public static byte[] splitBytes(byte[] bytes, int offset) {
        byte[] data = new byte[bytes.length - offset];
        for (int i = 1; i < bytes.length; i++) {
            data[i - 1] = bytes[i];
        }
        return data;
    }
}

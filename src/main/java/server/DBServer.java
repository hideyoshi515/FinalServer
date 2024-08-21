package server;

import dto.Player;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static dto.Player.bytesToHex;

public class DBServer {

    private static final int port = 1651;

    public static void DBServer() throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        // 프로토콜 인코더 및 디코더 추가
        acceptor.setHandler(new DBServerHandler());
        acceptor.bind(new InetSocketAddress(port));
        System.out.println("DB Server started on " + port);
    }

    static class DBServerHandler extends IoHandlerAdapter {

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            System.out.println("DB Session opened: " + session.getRemoteAddress());
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            System.out.println("DB Session closed: " + session.getRemoteAddress());
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            byte[] bytes;

            // 메시지가 byte[] 인지 확인
            if (message instanceof IoBuffer ioBuffer) {
                bytes = new byte[ioBuffer.remaining()];
                ioBuffer.get(bytes);
            } else if (message instanceof byte[]) {
                bytes = (byte[]) message;
            } else {
                throw new IllegalArgumentException("Unexpected message type: " + message.getClass().getName());
            }

            if (bytes.length < 1) {
                System.out.println("Received message too short.");
                return;
            }

            byte opcode = bytes[0];
            byte[] playerBytes;
            byte[] data = ByteBufferTool.splitBytes(bytes, 1);  // 첫 번째 바이트를 제외한 나머지 데이터 추출

            switch (opcode) {
                case 0:
                    Player.newPlayer(data);
                    System.out.println("DB Message Received: " + new String(data, StandardCharsets.UTF_8));
                    break;
                case 1:
                    Player.savePlayer(data);
                    System.out.println("DB Player saved: " + new String(data, StandardCharsets.UTF_8));
                    break;
                case 2:
                    playerBytes = Player.getPlayer(data);
                    IoBuffer buffer = IoBuffer.allocate(4 + playerBytes.length);
                    buffer.putInt(playerBytes.length);  // 데이터 길이 추가
                    buffer.put(playerBytes);            // 실제 데이터 추가
                    buffer.flip();
                    session.write(buffer);
                    System.out.println("[" + playerBytes.length + "] DB Packet Sent: " + bytesToHex(buffer.array()));
                    break;
                case 3:
                    String continuousCode = Player.setCode(data);
                    IoBuffer codeBuffer = IoBuffer.allocate(4 + continuousCode.length());
                    codeBuffer.putInt(continuousCode.length());
                    codeBuffer.put(continuousCode.getBytes(StandardCharsets.UTF_8));
                    codeBuffer.flip();
                    session.write(codeBuffer);
                    System.out.println("[continuousCode] DB Packet Sent: " + bytesToHex(codeBuffer.array()));
                    break;
                case 4:
                    playerBytes = Player.checkCode(data);
                    if (playerBytes != null) {
                        IoBuffer continueBuffer = IoBuffer.allocate(4 + playerBytes.length);
                        continueBuffer.putInt(playerBytes.length);
                        continueBuffer.put(playerBytes);
                        continueBuffer.flip();
                        session.write(continueBuffer);
                        System.out.println("[" + playerBytes.length + "] DB Packet Sent: " + bytesToHex(continueBuffer.array()));
                    }
                    break;
                default:
                    System.out.println("Unknown opcode: " + opcode);
            }
        }


        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            cause.printStackTrace();
            session.closeNow();
        }
    }
}

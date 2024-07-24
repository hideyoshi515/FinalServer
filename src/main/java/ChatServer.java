import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    private static final int port = 1650;
    private static final Set<IoSession> sessions = Collections.newSetFromMap(new ConcurrentHashMap<IoSession, Boolean>());

    public static void ChatServer() throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();

        // 프로토콜 인코더 및 디코더 추가
        DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();
        chain.addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));

        acceptor.setHandler(new ChatServerHandler());
        acceptor.bind(new InetSocketAddress(port));
        System.out.println("Server started on port: " + port);
    }

    static class ChatServerHandler extends IoHandlerAdapter {
        @Override
        public void sessionOpened(IoSession session) throws Exception {
            sessions.add(session);
            System.out.println("Session opened: " + session.getRemoteAddress());
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            sessions.remove(session);
            System.out.println("Session closed: " + session.getRemoteAddress());
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            String msg = message.toString();
            System.out.println("Received: " + msg);
            for (IoSession s : sessions) {
                s.write(msg);
            }
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            cause.printStackTrace();
            session.closeNow();
        }
    }
}

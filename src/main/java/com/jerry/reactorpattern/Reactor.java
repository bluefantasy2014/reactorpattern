package com.jerry.reactorpattern;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Reactor implements Runnable {
    final Selector selector;
    final ServerSocketChannel serverChannel;
    static final int WORKER_POOL_SIZE = 10;
    static ExecutorService workerPool;

    Reactor(int port) throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);

        // Register the server socket channel with interest-set set to ACCEPT operation
        SelectionKey sk = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        sk.attach(new Acceptor());
    }

    public void run() {
        try {
            while (true) {

                selector.select();
                Iterator it = selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey sk = (SelectionKey) it.next();
                    it.remove();
                    Runnable r = (Runnable) sk.attachment();
                    if (r != null)
                        r.run();
                }
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class Acceptor implements Runnable {
        public void run() {
            try {
                SocketChannel channel = serverChannel.accept();
                if (channel != null)
                    new Handler(selector, channel);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        workerPool = Executors.newFixedThreadPool(WORKER_POOL_SIZE);

        try {
            new Thread(new Reactor(9090)).start();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
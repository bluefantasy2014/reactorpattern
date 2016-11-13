package com.jerry.reactorpattern;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
 
class Handler implements Runnable {
	private static Logger LOG = Logger.getLogger(Handler.class); 
	
    final SocketChannel channel;
    final SelectionKey selKey;
 
    static final int READ_BUF_SIZE = 1024;
    static final int WRiTE_BUF_SIZE = 1024;
    ByteBuffer readBuf = ByteBuffer.allocate(READ_BUF_SIZE);
    ByteBuffer writeBuf = ByteBuffer.allocate(WRiTE_BUF_SIZE);
 
    Handler(Selector sel, SocketChannel sc) throws IOException {
        channel = sc;
        channel.configureBlocking(false);
 
        // Register the socket channel with interest-set set to READ operation
        // 只有从ServerSocketChannel.accept()方法返回拿到SocketChannel之后才能把它注册到selector上。 
        selKey = channel.register(sel, SelectionKey.OP_READ); 
        
        selKey.attach(this);
        
        //Q: 为什么还要调用interestOps？ register的时候不是已经指定了吗？ 
        selKey.interestOps(SelectionKey.OP_READ);
        sel.wakeup();
    }
 
    public void run() {
        try {
            if (selKey.isReadable())
                read();
            else if (selKey.isWritable())
                write();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
 
    // Process data by echoing input to output
    synchronized void process() {
        byte[] bytes;
 
        readBuf.flip();
        bytes = new byte[readBuf.remaining()];
        readBuf.get(bytes, 0, bytes.length);
        //J: bytes中存放的数据是真正读取到的数据。 
        LOG.info("process(): " + new String(bytes, Charset.forName("ISO-8859-1")));
 
        //J: 将读取到的数据放进writeBuf里。 
        writeBuf = ByteBuffer.wrap(bytes);
 
        // Set the key's interest to WRITE operation
        //J: 将interestOps设置为读 (sslectionKey.OP_WRITE)
//        LOG.info("before setting selKey's interestOps :" + selKey.interestOps()+ ", " + selKey);
        selKey.interestOps(SelectionKey.OP_WRITE);
//        LOG.info("after setting selKey's interestOps :" + selKey.interestOps());
        selKey.selector().wakeup();
    }
 
    //J: 每次这个channel上有数据到达就会执行这个方法。 
    synchronized void read() throws IOException {
        int numBytes;
 
        try {
            numBytes = channel.read(readBuf);
            LOG.info("read(): #bytes read into 'readBuf' buffer = " + numBytes);
 
            //J: Note: -1表示已经到了channel的end。 0表示没有读取到任何字节。含义是不一样的。 
            //J: 如果channel到了end of channel的话，就close这个channel，同时cancel selectionkey。 
            //J: Q: 这种情况什么时候发生？ 
            if (numBytes == -1) {
                selKey.cancel();
                channel.close();
                LOG.info("read(): client connection might have been dropped!");
            }
            else { //J: 如果读到了数据，就开始处理数据。 
                Reactor.workerPool.execute(new Runnable() {
                    public void run() {
                        process();
                    }
                });
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }
 
    //J: 每当channel上有写ready数据的时候触发
    void write() throws IOException {
        int numBytes = 0;
 
        try {
            numBytes = channel.write(writeBuf);
            LOG.info("write(): #bytes read from 'writeBuf' buffer = " + numBytes );
 
            //J: 当写成功的时候，清空buffer
            if (numBytes > 0) {
                readBuf.clear();
                writeBuf.clear();
 
                
                // Set the key's interest-set back to READ operation
//                LOG.info("write before setting selKey's interestOps :" + selKey.interestOps() + ", " + selKey);
                selKey.interestOps(SelectionKey.OP_READ);
//                LOG.info("write after setting selKey's interestOps :" + selKey.interestOps());
                selKey.selector().wakeup();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
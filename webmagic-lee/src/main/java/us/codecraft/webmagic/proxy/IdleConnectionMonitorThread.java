package us.codecraft.webmagic.proxy;

import org.apache.http.conn.HttpClientConnectionManager;

import java.util.concurrent.TimeUnit;

/**
 * @ClassName：IdleConnectionMonitorThread
 * @Description：关闭HTTP空闲连接、无效连接
 * @company:zhph
 * @author wlzheng
 * @date 2017-4-27 下午5:11:47
 */
public class IdleConnectionMonitorThread extends Thread {
	
	private final HttpClientConnectionManager connMgr;
    private volatile boolean shutdown;

    public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
        super();
        this.connMgr = connMgr;
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                synchronized (this) {
                    wait(5000);
                    // 关闭无效连接
                    connMgr.closeExpiredConnections();
                    // 可选, 关闭空闲超过60秒的
                    connMgr.closeIdleConnections(60, TimeUnit.SECONDS);
                }
            }
        } catch (InterruptedException ex) {
            // terminate
        	ex.printStackTrace();
        }
    }

    public void shutdown() {
        shutdown = true;
        synchronized (this) {
            notifyAll();
        }
    }

}

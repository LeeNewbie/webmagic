package us.codecraft.webmagic.proxy;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.selector.Json;
import us.codecraft.webmagic.util.ConfigReader;
import us.codecraft.webmagic.util.JedisUtils;
import us.codecraft.webmagic.utils.ProxyUtils;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


/**
 * @author liting
 * @ClassName：RedisProxyPool
 * @Description：
 * @company:zhph
 * @date 2017年9月4日 下午4:44:00
 */
public class RedisProxyPool {
    private static Logger logger = LoggerFactory.getLogger(RedisProxyPool.class);
    private static CloseableHttpClient httpclient = null;
    private static IdleConnectionMonitorThread scanThread = null;

    static {
        PoolingHttpClientConnectionManager cm = null;
        // SSLContext sslContext = createIgnoreVerifySSL();
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                // .register("https", new
                // SSLConnectionSocketFactory(sslContext))
                // .register("https", sslSocketFactory)
                .register("http", new PlainConnectionSocketFactory()).build();

        // 连接池设置
        cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // 最多400个连接
        cm.setMaxTotal(100);
        // 每个路由80个连接
        cm.setDefaultMaxPerRoute(80);

        // 定义keep alive保持策略
        ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                // Honor 'keep-alive' header
                HeaderElementIterator it = new BasicHeaderElementIterator(
                        response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        try {
                            return Long.parseLong(value) * 1000;
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
                HttpHost target = (HttpHost) context.getAttribute(HttpClientContext.HTTP_TARGET_HOST);
                if ("www.naughty-server.com".equalsIgnoreCase(target.getHostName())) {
                    // Keep alive for 5 seconds only
                    return 5 * 1000;
                } else {
                    // otherwise keep alive for 60 seconds
                    return 60 * 1000;
                }
            }
        };

        // 请求重试处理
        HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (executionCount >= 5) {
                    // Do not retry if over max retry count
                    return false;
                }
                if (exception instanceof InterruptedIOException) {
                    // Timeout
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {
                    // 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof UnknownHostException) {
                    // Unknown host
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {
                    // Connection refused
                    return false;
                }
                if (exception instanceof SocketTimeoutException) {
                    // Socket Timeout
                    return false;
                }
                if (exception instanceof SSLException) {
                    // SSL handshake exception
                    return false;
                }
                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
                if (idempotent) {
                    // Retry if the request is considered idempotent
                    return true;
                }
                return false;
            }
        };

        // 开启302重定向支持
        LaxRedirectStrategy rs = new LaxRedirectStrategy();
        // 创建client对象
        httpclient = HttpClients.custom().setConnectionManager(cm).setRedirectStrategy(rs)
                .setKeepAliveStrategy(myStrategy).setRetryHandler(myRetryHandler).build();
        // 扫描无效连接的线程
        scanThread = new IdleConnectionMonitorThread(cm);
        scanThread.start();

    }

    /**
     * 代理网站地址
     */
    private static final String data5u_api_url = ConfigReader.get("redis.proxyPool.proxySiteUrl");
    /**
     * 代理存货时长 秒
     */
    private static final int proxyIpActiveTime = Integer.parseInt(ConfigReader.get("redis.proxyPool.proxySiteIpActiveTime"));
    /**
     * redis set key
     */
    private String jedisPoolName = ConfigReader.get("redis.proxyPool.keyName");
    /**
     * 代理池维护的最少代理数量
     */
    private static int min_proxy_pool_num = Integer.parseInt(ConfigReader.get("redis.proxyPool.minSize"));
    /**
     * 检查代理有效间隔时间 毫秒
     */
    private static int validateProxyDeadTimeInterval = Integer.parseInt(ConfigReader.get("redis.proxyPool.validateProxyDeadTimeInterval"));
    /**
     * 检查代理池中代理数量间隔时间 毫秒
     */
    private static int checkProxyPoolNumInterval = Integer.parseInt(ConfigReader.get("redis.proxyPool.checkProxyPoolNumInterval"));

    private static Set<String> proxySet = new HashSet<String>(min_proxy_pool_num);
    private Timer timer = new Timer(true);
    private AtomicInteger countRequst = new AtomicInteger();
    private long startTime = System.currentTimeMillis();

    /**
     * 验证代理池中代理存活时间
     */
    private TimerTask validateProxyDeadTimeTask = new TimerTask() {

        @Override
        public void run() {
            logger.info("检查代理池中的代理是否存活...");
            Set<String> proxys = getAllProxy();
            if (proxys != null) {
                logger.info("当前代理池中代理数量: {}", proxys.size());
                ExecutorService executorSevice = Executors.newFixedThreadPool(5);
                for (String s : proxys) {
                    executorSevice.execute(new ValidateProxyThread(s));
                }
                executorSevice.shutdown();
                while (true) {
                    if (executorSevice.isTerminated()) {
                        break;
                    }
                    try {
                        executorSevice.awaitTermination(500, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                proxys = getAllProxy();
                if (proxys != null) {
                    logger.info("检查后,代理池中代理数量: {}", proxys.size());
                } else {
                    logger.info("当前代理池中暂无代理");
                }
            } else {
                logger.info("当前代理池中暂无代理");
            }

        }
    };

    /**
     * 验证代理池中代理数量
     */
    ExecutorService executor = Executors.newFixedThreadPool(5);

    private TimerTask checkProxyPoolNumTask = new TimerTask() {

        @Override
        public void run() {
            if (getAllProxyNum() < min_proxy_pool_num) {
                executor.execute(new Runnable() {
                    public void run() {
                        String response = getProxyFromData5u();
//						String response = getProxyFromKuaiDaili();
//						String response = getProxyFromBugng();
                        if (StringUtils.isEmpty(response)) {
//							timer.cancel();
                            logger.error("获取代理出错,请联系管理员");
                            return;
                        } else {
//							while(temIp.equals(response)){
//								response = getProxyFromData5u();
//								System.out.println(response);
//								try {
//									Thread.sleep(1000);
//								} catch (InterruptedException e) {
//									e.printStackTrace();
//								}
//							}
//							temIp = response;
                            String[] res = response.split("\\n");
                            for (String ip : res) {
                                if (validateProxyIpFormat(ip)) {
                                    addProxy(ip);
                                } else {
                                    logger.warn("获取的代理ip格式不正确, {}", ip);
                                }
                            }
                        }
                    }
                });
            }
        }
    };


    /**
     *
     */
    public class ValidateProxyThread implements Runnable {
        private String proxy;

        public ValidateProxyThread(String proxy) {
            this.proxy = proxy;
        }

        @Override
        public void run() {
            Proxy p = JSON.parseObject(proxy, Proxy.class);
            if (!validataProxyTimeOut(p) || !validateProxy(p)) {
                removeProxy(proxy);
            }
        }
    }

    public RedisProxyPool() {
        timer.schedule(validateProxyDeadTimeTask, 10, validateProxyDeadTimeInterval);
        timer.schedule(checkProxyPoolNumTask, 10, checkProxyPoolNumInterval);
    }

    public RedisProxyPool(boolean isUseProxy) {
        if (isUseProxy) {
            timer.schedule(validateProxyDeadTimeTask, 0, validateProxyDeadTimeInterval);
            timer.schedule(checkProxyPoolNumTask, 0, checkProxyPoolNumInterval);
        }
    }

    public RedisProxyPool(String jedisPoolName) {
        this.jedisPoolName = jedisPoolName;
//		while (getAllProxyNum() < min_proxy_pool_num) {
//			String response = getProxyFromData5u();
//			if (response.indexOf("已经过期") != -1) {
//				logger.warn("代理ip欠费过期");
//				logger.warn(response);
//				return;
//			}
//			addProxy(response);
//			try {
//				Thread.sleep(1000*3);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
        timer.schedule(validateProxyDeadTimeTask, 0, validateProxyDeadTimeInterval);
        timer.schedule(checkProxyPoolNumTask, 0, checkProxyPoolNumInterval);
    }

    /**
     * @return
     */
    public Proxy getProxy() {
        String value = JedisUtils.getSetMember(jedisPoolName);
        if (value == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getProxy();
        }
        Proxy proxy = JSON.parseObject(value, Proxy.class);
        if (validataProxyTimeOut(proxy) && validateProxy(proxy)) {
            return proxy;
        } else {
            return getProxy();
        }

    }

    /**
     * @param ip
     */
    public void addProxy(String ip) {
        Proxy proxy = changeIpToProxy(ip);
        if (validataProxyTimeOut(proxy) && validateProxy(proxy)) {
            JedisUtils.setSetAdd(jedisPoolName, JSON.toJSONString(proxy));
        }
    }

    public void returnProxy(Proxy proxy) {
        if (validataProxyTimeOut(proxy) && validateProxy(proxy)) {
            JedisUtils.setSetAdd(jedisPoolName, JSON.toJSONString(proxy));
        }
    }

    public void removeProxy(String proxy) {
        JedisUtils.delSetMember(jedisPoolName, proxy);
    }

    /**
     * @param ip
     * @return
     */
    private Proxy changeIpToProxy(String ip) {
        Proxy proxy = null;
        if (ip.indexOf(",") != -1) {
            String ipAndPort = ip.split(",")[0];
            long time = Long.parseLong(ip.split(",")[1]) + System.currentTimeMillis();
            String address = ipAndPort.split(":")[0];
            int port = Integer.parseInt(ipAndPort.split(":")[1]);
            proxy = new Proxy(address, port, time);
        } else {
            String address = ip.split(":")[0];
            int port = Integer.parseInt(ip.split(":")[1]);
            proxy = new Proxy(address, port);
        }
        return proxy;
    }

    /**
     * @param proxy
     * @return
     */
    private boolean validateProxy(Proxy proxy) {
        InetAddress localAddr = null;
        try {
            localAddr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (localAddr == null) {
            logger.error("cannot get local IP");
            return false;
        }
        boolean isReachable = false;
        Socket socket = null;
        try {
            socket = new Socket();
            socket.bind(new InetSocketAddress(localAddr, 0));
            InetSocketAddress endpointSocketAddr = new InetSocketAddress(proxy.getHost(), proxy.getPort());
            socket.connect(endpointSocketAddr, 5000);
            //logger.info("SUCCESS - connection established! Local: " + localAddr.getHostAddress() + " remote: " + proxy);
            isReachable = true;
        } catch (IOException e) {
            logger.warn("FAILRE - CAN not connect! Local: " + localAddr.getHostAddress() + " remote: " + proxy);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.warn("Error occurred while closing socket of validating proxy", e);
                }
            }
        }
        return isReachable;
    }

    /**
     * @return
     */
    public long getAllProxyNum() {
        return JedisUtils.getSetNum(jedisPoolName);
    }

    /**
     * @return
     */
    public Set<String> getAllProxy() {
        return JedisUtils.getSet(jedisPoolName);
    }

    /**
     * @param proxy
     * @return
     */
    private boolean validataProxyTimeOut(Proxy proxy) {
        if (proxy.getTimeout() < System.currentTimeMillis()) {
            return false;
        }
        return true;
    }

    HttpClientContext context = new HttpClientContext();
    /**
     * @return
     */
    private String getProxyFromData5u() {
        logger.info("请求代理次数统计:" + countRequst.get());
        long time = System.currentTimeMillis() - startTime;
        logger.info("频次:{}秒/次", time / (float) countRequst.get() / 1000);
        logger.info("从data5u获取代理ip");
        CloseableHttpResponse response = null;
        HttpGet httpGet = new HttpGet(data5u_api_url);
        httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpGet.setHeader("Accept-Encoding", "gzip, deflate");
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3080.5 Safari/537.36");
        httpGet.setHeader("Host", "api.ip.data5u.com");
        httpGet.setHeader("Connection", "keep-alive");
        try {
            response = httpclient.execute(httpGet,context);
            countRequst.addAndGet(1);
            if (response != null) {
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity);
                if (content.indexOf("已经过期") != -1) {
                    logger.warn(JSON.parseObject(content).getString("msg"));
//					timer.cancel();
                    return "";
                }
                logger.info("getProxyFromData5u reponse content : " + content);
                return content;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            httpGet.releaseConnection();
        }
        return "";
    }

    private String getProxyFromKuaiDaili() {
        logger.debug("从快代理获取代理ip");
        CloseableHttpResponse response = null;
        HttpGet httpGet = new HttpGet("http://svip.kuaidaili.com/api/getproxy/?orderid=931001722625954&num=10&area=%E4%B8%AD%E5%9B%BD&b_pcchrome=1&b_pcie=1&b_pcff=1&protocol=2&method=2&an_an=1&an_ha=1&quality=2&sort=1&sep=1");
        httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpGet.setHeader("Accept-Encoding", "gzip, deflate");
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3080.5 Safari/537.36");
        httpGet.setHeader("Connection", "keep-alive");
        try {
            response = httpclient.execute(httpGet);
            if (response != null) {
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity);
                if (content.indexOf("已经过期") != -1) {
                    logger.warn(JSON.parseObject(content).getString("msg"));
//					timer.cancel();
                    return "";
                }
                logger.info("getProxyFromKuaiDaili reponse content : " + content);
                return content;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            httpGet.releaseConnection();
        }
        return "";
    }


    /**
     * @throws
     * @Title: getProxyFromBugng
     * @param：@return
     * @return：String
     * @Description：TODO(从Bugng获取代理ip)
     * @author liting
     * @date 2017年9月6日 上午10:18:21
     */
    private String getProxyFromBugng() {
        logger.debug("从Bugng获取代理ip");
        CloseableHttpResponse response = null;
        HttpGet httpGet = new HttpGet("http://www.bugng.com/api/getproxy/json?num=20&anonymity=1&type=2");
        try {
            response = httpclient.execute(httpGet);
            if (response != null) {
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity);
                if (StringUtils.isEmpty(content)) {
                    logger.warn("代理已失效");
                    return "";
                }
                String proxy_list = JSON.parseObject(content).getJSONObject("data").getString("proxy_list");
                proxy_list = proxy_list.replaceAll("\",\"", ",66666\n").replace("[\"", "").replace("\"]", ",66666\n");
                System.out.println(proxy_list);
//				content = content.replaceAll(".*", ",66666");
                logger.info("reponse content : " + content);
                return proxy_list;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            httpGet.releaseConnection();
        }
        return "";
    }

    /**
     * @throws
     * @Title: getProxyFromBugng
     * @param：@return
     * @return：String
     * @Description：TODO(从Bugng获取代理ip)
     * @author liting
     * @date 2017年9月6日 上午10:18:21
     */
    private String getProxyFromXunDaili() {
        logger.debug("从Bugng获取代理ip");
        CloseableHttpResponse response = null;
        HttpGet httpGet = new HttpGet("http://api.xdaili.cn/xdaili-api//greatRecharge/getGreatIp?spiderId=90913be0dd7e4a1098f15accc7f8d14b&orderno=MF20171176425XecaGT&returnType=1&count=10");
        httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpGet.setHeader("Accept-Encoding", "gzip, deflate");
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3080.5 Safari/537.36");
        httpGet.setHeader("Connection", "keep-alive");
        try {
            response = httpclient.execute(httpGet);
            if (response != null) {
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity);
                if (content.indexOf("提取太频繁,请按规定频率提取") != -1) {
                    logger.warn(JSON.parseObject(content).getString("msg"));
//					timer.cancel();
                    return "";
                }
                logger.info("getProxyFromXunDaili reponse content : " + content);
                return content;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            httpGet.releaseConnection();
        }
        return "";
    }

    private String getProxyFromDailiyun() {
        logger.debug("从Bugng获取代理ip");
        CloseableHttpResponse response = null;
        HttpGet httpGet = new HttpGet("http://dly.134t.com/query.txt?key=NP15082BC4&word=&count=5");
        httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpGet.setHeader("Accept-Encoding", "gzip, deflate");
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3080.5 Safari/537.36");
        httpGet.setHeader("Connection", "keep-alive");
        try {
            response = httpclient.execute(httpGet);
            if (response != null) {
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity);
                if (content.indexOf("提取太频繁,请按规定频率提取") != -1) {
                    logger.warn(JSON.parseObject(content).getString("msg"));
//					timer.cancel();
                    return "";
                }
                logger.info("getProxyFromXunDaili reponse content : " + content);
                return content;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            httpGet.releaseConnection();
        }
        return "";
    }

    private static final String VALIDATE_PROXY_URL = ConfigReader.get("redis.proxyPool.validateUrl");
//	private static final String VALIDATE_PROXY_REG = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})] 来自：([\\u4e00-\\u9fa5]+) ([\\u4e00-\\u9fa5]+|)<";

    /**
     * @throws
     * @Title: validateProxy
     * @param：@param host
     * @param：@return
     * @return：boolean
     * @Description：TODO(验证代理)
     * @author liting
     * @date 2017年9月6日 上午10:22:06
     */
    private boolean validateProxy(HttpHost host) {
        logger.info("验证proxy {} 是否有效", host.getHostName());
        CloseableHttpResponse response = null;
        HttpGet httpGet = new HttpGet(VALIDATE_PROXY_URL);
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(3000)//设置连接超时时间
                .setSocketTimeout(2000)//设置通信连接超时时间
                .setConnectionRequestTimeout(3000)
                .setProxy(host)
                .build();
        httpGet.setConfig(config);
        try {
            response = httpclient.execute(httpGet);
            if (response != null) {
//				HttpEntity entity = response.getEntity();
//				String content = EntityUtils.toString(entity,"gb2312");
//				Pattern p = Pattern.compile(VALIDATE_PROXY_REG);
//				Matcher m = p.matcher(content);
//				while(m.find()){
//					System.out.println("获取到ip："+m.group(1));
//					System.out.println("地址： "+m.group(2));
//					System.out.println("运营商： "+m.group(3));
//				}
                logger.info("proxy {} 有效", host.getHostName());
                return true;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
//			e.printStackTrace();
            logger.info("get to proxy {} timeout", host.getHostName());
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            httpGet.releaseConnection();
        }
        logger.info("proxy {} 已失效", host.getHostName());
        return false;
    }

    private static Pattern PROXYIP_PATTERN = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+,\\d+|\\d+)");

    private static boolean validateProxyIpFormat(String ip) {
        if (PROXYIP_PATTERN.matcher(ip).matches()) {
            return true;
        } else {
            return false;
        }
    }

    //test
    public static void main(String[] args) {
        new RedisProxyPool();
//		System.out.println(validateProxyIpFormat("123.162.193.228:34952"));
    }
}

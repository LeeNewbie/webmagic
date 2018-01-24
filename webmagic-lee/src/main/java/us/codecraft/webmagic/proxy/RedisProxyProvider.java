package us.codecraft.webmagic.proxy;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Task;

/**
 * @package: us.codecraft.webmagic.proxy
 * @ClassName: RedisProxyProvider
 * @Description:
 * @author: liting
 * @date: 2018-01-23 14:16
 */
public class RedisProxyProvider implements ProxyProvider{
    private RedisProxyPool proxyPool = new RedisProxyPool(false);

    @Override
    public void returnProxy(Proxy proxy, Page page, Task task) {
//        proxyPool.returnProxy(proxy);
    }

    @Override
    public Proxy getProxy(Task task) {
        return  proxyPool.getProxy();
    }
}

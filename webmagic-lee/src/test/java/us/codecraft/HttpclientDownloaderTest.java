package us.codecraft;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.proxy.RedisProxyProvider;

/**
 * @package: us.codecraft
 * @ClassName: HttpclientDownloaderTest
 * @Description:
 * @author: liting
 * @date: 2018-01-24 10:49
 */
public class HttpclientDownloaderTest {
    public static void main(String[] args) {
        HttpClientDownloader downloader = new HttpClientDownloader();
//        downloader.setProxyProvider(new RedisProxyProvider());
        Request request = new Request("http://res3.d.cn/android/new/game/60/81260/yj_1515722181542.apk")
                .setRequestAsAFile(true)
                .setRequestFileSavePath("D:\\data\\yj_1515722181542.apk");
        downloader.download(request, new Task() {
            @Override
            public String getUUID() {
                return "tt";
            }

            @Override
            public Site getSite() {
                return Site.me();
            }
        });
    }
}

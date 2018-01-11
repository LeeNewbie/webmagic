package us.codecraft.webmagic.downloader;

/**
 * @package: us.codecraft.webmagic.downloader
 * @ClassName: PhantomJsDownloaderTest
 * @Description:
 * @author: liting
 * @date: 2018-01-11 10:45
 */

import org.junit.Test;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;

/**
 * Created by lee on 2018年01月11日
 */
public class PhantomJsDownloaderTest {
    @Test
    public void downPhantomJSDownloaderloadTest() {
        PhantomJSDownloader downloader = new PhantomJSDownloader();
        String page = downloader.getPage(new Request("http://www.baidu.com"));
        System.out.println(page);
    }

}

package us.codecraft.webmagic.model.samples;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.model.OOSpider;
import us.codecraft.webmagic.model.annotation.ExtractBy;
import us.codecraft.webmagic.selector.Html;

import java.io.IOException;

/**
 * @author code4crafter@gmail.com
 */
public class BaiduNews {

    @ExtractBy("//h3[@class='c-title']/a/text()")
    private String name;

    @ExtractBy("//div[@class='c-summary']/text()")
    private String description;

    @Override
    public String toString() {
        return "BaiduNews{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public static void main(String[] args) throws Exception {
        OOSpider ooSpider = OOSpider.create(Site.me().setSleepTime(0)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36"), BaiduNews.class);
        //single download
        BaiduNews baike = ooSpider.<BaiduNews>get("http://news.baidu.com/ns?word=加密货币");
        System.out.println(baike);

        ooSpider.close();


    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
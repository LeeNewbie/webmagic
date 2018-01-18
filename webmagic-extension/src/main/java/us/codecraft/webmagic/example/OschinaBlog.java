package us.codecraft.webmagic.example;

import com.alibaba.fastjson.JSONObject;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.model.ConsolePageModelPipeline;
import us.codecraft.webmagic.model.OOSpider;
import us.codecraft.webmagic.model.annotation.ExtractBy;
import us.codecraft.webmagic.model.annotation.Formatter;
import us.codecraft.webmagic.model.annotation.TargetUrl;
import us.codecraft.webmagic.pipeline.JsonFilePageModelPipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

import java.util.Date;
import java.util.List;

/**
 * @author code4crafter@gmail.com <br>
 * @since 0.3.2
 */
@TargetUrl("https://my.oschina.net/flashsword/blog/\\d+")
//@TargetUrl("http://my.oschina.net/flashsword/blog")
public class OschinaBlog {

    @ExtractBy("//title/text()")
    private String title;

    @ExtractBy(value = "div.BlogContent", type = ExtractBy.Type.Css)
    private String content;

    @ExtractBy(value = "//div[@class='BlogTags']/a/text()", multi = true)
    private List<String> tags;

//    @ExtractBy("//div[@class='BlogStat']/regex('\\d+-\\d+-\\d+\\s+\\d+:\\d+')")
    private Date date;

    public static void main(String[] args) {
//        new JsonFilePageModelPipeline("D:\\data\\webmagic\\").process(new JSONObject(), new Task() {
//            @Override
//            public String getUUID() {
//                return "uuid";
//            }
//
//            @Override
//            public Site getSite() {
//                return null;
//            }
//        });
        //results will be saved to "/data/webmagic/" in json format
        OOSpider.create(Site.me(), new JsonFilePageModelPipeline("D:\\data\\webmagic\\"), OschinaBlog.class)
                .addUrl("http://my.oschina.net/flashsword/blog").run();
//        OschinaBlog blog = OOSpider.create(Site.me(), new JsonFilePageModelPipeline("D:\\data\\webmagic\\"), OschinaBlog.class)
//                .<OschinaBlog>get("http://my.oschina.net/flashsword/blog");
//        System.out.println(blog);
//        OOSpider.create(Site.me(), new ConsolePageModelPipeline(), OschinaBlog.class)
//                .addUrl("https://my.oschina.net/flashsword/blog").run();
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public List<String> getTags() {
        return tags;
    }

    public Date getDate() {
        return date;
    }

}

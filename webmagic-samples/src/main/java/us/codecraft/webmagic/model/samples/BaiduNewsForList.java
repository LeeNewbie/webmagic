package us.codecraft.webmagic.model.samples;

import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.model.OOSpider;
import us.codecraft.webmagic.model.annotation.ExtractBy;

import java.util.Collections;
import java.util.List;

/**
 * @author code4crafter@gmail.com
 */
// TODO: 2018/1/16 0016 实现列表页抓取
public class BaiduNewsForList {

    @ExtractBy("//h3[@class='c-title']/a/text()")
    private List<String> name;

    @ExtractBy("//div[@class='c-summary']/text()")
    private List<String> description;

    @Override
    public String toString() {
        return "BaiduNewsForList{" +
                "name='" + name.size() + '\'' +
                ", description='" + description.size() + '\'' +
                '}';
    }

    public static void main(String[] args) throws Exception {
        OOSpider ooSpider = OOSpider.create(Site.me().setSleepTime(0)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36"), BaiduNewsForList.class);
        //single download
        BaiduNewsForList baike = ooSpider.<BaiduNewsForList>get("http://news.baidu.com/ns?word=加密货币");
        System.out.println(baike);

        ooSpider.close();


    }

    public List<String> getName() {
        return name;
    }

    public List<String> getDescription() {
        return description;
    }
}
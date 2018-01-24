package us.codecraft.webmagic.samples;

import com.alibaba.fastjson.JSON;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.model.ITHomeArticle;
import us.codecraft.webmagic.model.ITHomeComment;
import us.codecraft.webmagic.pipeline.FilePipeLinePlus;
import us.codecraft.webmagic.pipeline.FilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.PriorityScheduler;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.HttpConstant;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @package: us.codecraft.webmagic.samples
 * @ClassName: IThomePageProcessor
 * @Description:
 * @author: liting
 * @date: 2018-01-17 15:07
 */
public class IThomePageProcessor implements PageProcessor {
    public static final String BASE_COMMENT_COUNT_URL = "https://dyn.ithome.com/api/comment/count?newsid=";
    public static final String BASE_COMMENT_URL = "https://dyn.ithome.com/comment/";
    public static final String BASE_COMMENT_DETAIL_URL = "https://dyn.ithome.com/ithome/getajaxdata.aspx";

    public static final String POST_PARAM_HOTCOMMENT = "hotcomment";
    public static final String POST_PARAM_COMMENTPAGE = "commentpage";

    public static final String REG_DETAIL_PAGE = "https://www.ithome.com/html/\\w+/\\d+.htm";
    public static final String REG_DETAIL_PAGE_ID = "https://www.ithome.com/html/\\w+/(\\d+).htm";

    public static final String REG_HOME_PAGE = "https://www.ithome.com";
    public static final String REG_COMMENT_COUNT_PAGE = "https://dyn.ithome.com/api/comment/count\\?newsid=\\d+";
    public static final String REG_COMMENT_DETAIL_PAGE = "https://dyn.ithome.com/comment/\\d+";

    public static final String EXTRA_KEY_ID = "id";
    public static final String EXTRA_KEY_COMMENT_TYPE = "commentType";
    public static final String EXTRA_KEY_ITHOME = "itHome";
    public static final String EXTRA_KEY_HASH = "hash";

    private boolean isSingle = false;

    private Site site = Site.me()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36")
            .setCharset("utf-8");

    @Override
    public void process(Page page) {
        if (REG_HOME_PAGE.equals(page.getUrl().toString())) {
            //主页
            List<String> ls = page.getHtml().links().regex(REG_DETAIL_PAGE).all();
            for (String link : ls) {
                String id = new PlainText(link).regex(REG_DETAIL_PAGE_ID).get();
                Request request = new Request(link).putExtra(EXTRA_KEY_ID, id);
                page.addTargetRequest(request);
            }
        } else if (page.getUrl().regex(REG_DETAIL_PAGE).match()) {
            //列表页
            String id;
            if (page.getRequest().getExtra(EXTRA_KEY_ID) == null) {
                id = page.getUrl().regex(REG_DETAIL_PAGE_ID).get();
            } else {
                id = page.getRequest().getExtra(EXTRA_KEY_ID).toString();
            }

            String title = page.getHtml().xpath("//div[@class='post_title']/h1/text()").toString();
            String content = page.getHtml().xpath("//*[@id='paragraph']/tidyText()").toString();
            String time = page.getHtml().xpath("//*[@id='pubtime_baidu']/text()").toString();
            String source = page.getHtml().xpath("//*[@id='source_baidu']/a/text()").toString();
            String author = page.getHtml().xpath("//*[@id='author_baidu']/strong/text()").toString();
            String editor = page.getHtml().xpath("//*[@id='editor_baidu']/strong/text()").toString();
            List<String> imgLinks = page.getHtml().xpath("//*[@id='paragraph']//img/@data-original").all();

            ITHomeArticle itHomeArticle = new ITHomeArticle();
            itHomeArticle.setId(id);
            itHomeArticle.setTitle(title);
            itHomeArticle.setContent(content);
            itHomeArticle.setTime(time);
            itHomeArticle.setSource(source);
            itHomeArticle.setAuthor(author);
            itHomeArticle.setEditor(editor);
            itHomeArticle.setImgLinks(imgLinks);

            String commentCountUrl = BASE_COMMENT_COUNT_URL + id;
            Request request = new Request(commentCountUrl);
            request.putExtra(EXTRA_KEY_ID, id);
            request.putExtra(EXTRA_KEY_ITHOME, itHomeArticle);
            request.setPriority(100);
            page.addTargetRequest(request);

            List<String> ls = page.getHtml().links().regex(REG_DETAIL_PAGE).all();
            for (String link : ls) {
                String idElse = new PlainText(link).regex(REG_DETAIL_PAGE_ID).get();
                Request requestElse = new Request(link).putExtra(EXTRA_KEY_ID, idElse);
                page.addTargetRequest(requestElse);
            }

            for (String imgLink : imgLinks) {
                Request requestElse = new Request(imgLink).setRequestAsAFile(true).setPriority(10000);
                if (imgLink.indexOf("@") != -1) {
                    imgLink = imgLink.substring(imgLink.lastIndexOf("/"), imgLink.indexOf("@"));
                } else {
                    imgLink = imgLink.substring(imgLink.lastIndexOf("/"));
                }
                requestElse.setRequestFileSavePath("D:\\data\\"+getSite().getDomain()+"\\"+id+"\\"+imgLink);
                page.addTargetRequest(requestElse);
            }
        } else if (page.getUrl().regex(REG_COMMENT_COUNT_PAGE).match()) {
            //评论个数统计
            String id = page.getRequest().getExtra(EXTRA_KEY_ID).toString();
            ITHomeArticle itHomeArticle = (ITHomeArticle)page.getRequest().getExtra(EXTRA_KEY_ITHOME);
            String commentCount = page.getHtml().regex("innerHTML = '(\\d+)'").get();
            itHomeArticle.setCommentcount(commentCount);
            String commentUrl = BASE_COMMENT_URL + id;
            Request request = new Request(commentUrl)
                    .setPriority(100)
                    .putExtra(EXTRA_KEY_ID, id)
                    .putExtra(EXTRA_KEY_ITHOME,itHomeArticle);
            page.addTargetRequest(request);
        } else if (page.getUrl().regex(REG_COMMENT_DETAIL_PAGE).match()) {
            //评论展示页
            String hash = page.getHtml().css("#hash", "value").get();
            String id = page.getRequest().getExtra("id").toString();
            Map<String, Object> params = new HashMap<String, Object>(2);
            params.put("newsID", id);
            params.put("type", POST_PARAM_HOTCOMMENT);
            Request request = new Request(BASE_COMMENT_DETAIL_URL)
                    .setMethod(HttpConstant.Method.POST)
                    .setPriority(100)
                    .putExtra(EXTRA_KEY_HASH, hash)
                    .putExtra(EXTRA_KEY_ID, id)
                    .putExtra(EXTRA_KEY_COMMENT_TYPE, POST_PARAM_HOTCOMMENT)
                    .putExtra(EXTRA_KEY_ITHOME,page.getRequest().getExtra(EXTRA_KEY_ITHOME));
            doPostRequstForComment(page, request, params);

        } else if (BASE_COMMENT_DETAIL_URL.equals(page.getUrl().get())) {
            //评论详情页
            String hash = page.getRequest().getExtra(EXTRA_KEY_HASH).toString();
            String id = page.getRequest().getExtra(EXTRA_KEY_ID).toString();
            ITHomeArticle itHomeArticle = (ITHomeArticle)page.getRequest().getExtra(EXTRA_KEY_ITHOME);
            if (page.getRequest().getExtra(EXTRA_KEY_COMMENT_TYPE) != null) {
                itHomeArticle.setHotComments(processComment(page));
                Map<String, Object> params = new HashMap<String, Object>(5);
                params.put("newsID", id);
                params.put("type", POST_PARAM_COMMENTPAGE);
                params.put("hash", hash);
                params.put("page", "1");
                params.put("order", "true");
                Request request = new Request(BASE_COMMENT_DETAIL_URL)
                        .setMethod(HttpConstant.Method.POST)
                        .setPriority(100)
                        .putExtra(EXTRA_KEY_ID,id)
                        .putExtra(EXTRA_KEY_HASH,hash)
                        .putExtra(EXTRA_KEY_ITHOME,itHomeArticle);
                doPostRequstForComment(page, request, params);
            } else {
                page.getHtml();
                itHomeArticle.setComments(processComment(page));
                page.putField("itHomeArticle", itHomeArticle);
                page.putField("title",itHomeArticle.getTitle());
            }
        }

    }

    /**
     *
     * @param page
     * @param request
     * @param params
     */
    private void doPostRequstForComment(Page page, Request request, Map<String, Object> params) {
        HttpRequestBody commentBody = null;
        try {
            commentBody = HttpRequestBody.form(params, "utf-8");
            request.setRequestBody(commentBody);
            page.addTargetRequest(request);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param page
     * @return
     */
    private List<ITHomeComment> processComment(Page page) {
        List<ITHomeComment> comments = new ArrayList<ITHomeComment>(20);
        List<String> lis = page.getHtml().xpath("//li[@class='entry']").all();
        for (String li : lis) {
            Html liNode = new Html(li);
            String id = liNode.xpath("//div/a/@title").regex("\\d+").get();
            String floor = liNode.xpath("//div/[@class]/strong/text()").get();
            String nick = liNode.xpath("//div/[@class]/strong/a/text()").get();
            String mobile = liNode.xpath("//div/[@class]/span/a/text()").get();
            if (liNode.xpath("//div[@class]/span[@class='posandtime']/text()").get() == null) {
                System.out.println("====");
            }
            String[] posAndTime = liNode.xpath("//div[@class]/span[@class='posandtime']/text()").get().split(" ");
            String pos = posAndTime[0];
            String time = posAndTime[1];
            String comment = liNode.xpath("//div/p/text()").get();
            ITHomeComment itHomeComment = new ITHomeComment();
            itHomeComment.setComment(comment);
            itHomeComment.setUserId(id);
            itHomeComment.setFloor(floor);
            itHomeComment.setNick(nick);
            itHomeComment.setMobile(mobile);
            itHomeComment.setPos(pos);
            itHomeComment.setTime(time);
            itHomeComment.setComment(comment);
            comments.add(itHomeComment);
        }
        return comments;
    }

    @Override
    public Site getSite() {
        return site;
    }

    public boolean isSingle() {
        return isSingle;
    }

    public void setSingle(boolean single) {
        isSingle = single;
    }

    public static void main(String[] args) {
        Spider.create(new IThomePageProcessor())
                .addPipeline(new FilePipeLinePlus("D:\\data\\webmagic"))
                .setScheduler(new PriorityScheduler())
                .thread(5)
                .addUrl("https://www.ithome.com").start();
    }
}

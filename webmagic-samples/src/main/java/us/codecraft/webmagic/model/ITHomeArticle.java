package us.codecraft.webmagic.model;

import java.util.List;

/**
 * @package: us.codecraft.webmagic.model
 * @ClassName: ITHomeArticle
 * @Description:
 * @author: liting
 * @date: 2018-01-18 9:49
 */
public class ITHomeArticle {
    /**
     * 文章id
     */
    private String id;
    /**
     * 标题
     */
    private String title;
    /**
     * 内容
     */
    private String content;
    /**
     * 图片链接
     */
    private List<String> imgLinks;
    /**
     * 发布时间
     */
    private String time;
    /**
     * 文章来源
     */
    private String source;
    /**
     * 作者
     */
    private String author;
    /**
     * 编辑
     */
    private String editor;
    /**
     * 评论量
     */
    private String commentcount;
    /**
     * 热门评论
     */
    private List<ITHomeComment> hotComments;
    /**
     * 评论
     */
    private List<ITHomeComment> comments;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImgLinks() {
        return imgLinks;
    }

    public void setImgLinks(List<String> imgLinks) {
        this.imgLinks = imgLinks;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
    }

    public void setCommentcount(String commentcount) {
        this.commentcount = commentcount;
    }

    public List<ITHomeComment> getHotComments() {
        return hotComments;
    }

    public void setHotComments(List<ITHomeComment> hotComments) {
        this.hotComments = hotComments;
    }

    public List<ITHomeComment> getComments() {
        return comments;
    }

    public void setComments(List<ITHomeComment> comments) {
        this.comments = comments;
    }
}

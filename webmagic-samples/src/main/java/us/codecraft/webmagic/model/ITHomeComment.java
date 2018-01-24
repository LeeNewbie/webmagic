package us.codecraft.webmagic.model;

/**
 * @package: us.codecraft.webmagic.model
 * @ClassName: ITHomeComment
 * @Description:
 * @author: liting
 * @date: 2018-01-18 10:45
 */
public class ITHomeComment {
    /**
     * 楼层
     */
    private String floor;
    /**
     * 昵称
     */
    private String nick;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 手机类型
     */
    private String mobile;
    /**
     * 地点
     */
    private String pos;
    /**
     * 时间
     */
    private String time;
    /**
     * 评论内容
     */
    private String comment;

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

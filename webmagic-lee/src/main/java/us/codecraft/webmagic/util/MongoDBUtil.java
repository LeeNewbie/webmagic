package us.codecraft.webmagic.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import org.bson.BasicBSONObject;
import org.bson.conversions.Bson;

import org.bson.Document;
import org.bson.types.ObjectId;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @package: us.codecraft.webmagic.util
 * @ClassName: MongoDBUtil
 * @Description:
 * @author: liting
 * @date: 2018-01-22 11:53
 */
public enum MongoDBUtil {
    /**
     * 定义一个枚举的元素，它代表此类的一个实例
     */
    instance;

    private static MongoClient mongoClient;

    static {
        System.out.println("===============MongoDBUtil初始化========================");
        String ip = "192.168.18.35";
        int port = 27017;
        ServerAddress address = new ServerAddress(ip, port);
        // 大部分用户使用mongodb都在安全内网下，但如果将mongodb设为安全验证模式，就需要在客户端提供用户名和密码：
        // boolean auth = db.authenticate(myUserName, myPassword);
        MongoClientOptions.Builder options = new MongoClientOptions.Builder();
        options.cursorFinalizerEnabled(true);
        // options.autoConnectRetry(true);// 自动重连true
        // options.maxAutoConnectRetryTime(10); // the maximum auto connect retry time
        // 连接池设置为300个连接,默认为100
        options.connectionsPerHost(300);
        // 连接超时，推荐>3000毫秒
        options.connectTimeout(30000);
        options.maxWaitTime(5000);
        // 套接字超时时间，0无限制
        options.socketTimeout(0);
        // 线程队列数，如果连接线程排满了队列就会抛出“Out of semaphores to get db”错误。
        options.threadsAllowedToBlockForConnectionMultiplier(5000);
        options.writeConcern(WriteConcern.SAFE);
        mongoClient = new MongoClient(address, options.build());
    }

    // ------------------------------------共用方法---------------------------------------------------

    /**
     * 获取DB实例 - 指定DB
     *
     * @param dbName
     * @return
     */
    public MongoDatabase getDB(String dbName) {
        if (dbName != null && !"".equals(dbName)) {
            MongoDatabase database = mongoClient.getDatabase(dbName);
            return database;
        }
        return null;
    }

    /**
     * 获取collection对象 - 指定Collection
     *
     * @param collName
     * @return
     */
    public MongoCollection<Document> getCollection(String dbName, String collName) {
        if (null == collName || "".equals(collName)) {
            return null;
        }
        if (null == dbName || "".equals(dbName)) {
            return null;
        }
        MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(collName);
        return collection;
    }

    /**
     * 查询DB下的所有表名
     */
    public List<String> getAllCollections(String dbName) {
        MongoIterable<String> colls = getDB(dbName).listCollectionNames();
        List<String> _list = new ArrayList<String>();
        for (String s : colls) {
            _list.add(s);
        }
        return _list;
    }

    /**
     * 获取所有数据库名称列表
     *
     * @return
     */
    public MongoIterable<String> getAllDBNames() {
        MongoIterable<String> s = mongoClient.listDatabaseNames();
        return s;
    }

    /**
     * 删除一个数据库
     */
    public void dropDB(String dbName) {
        getDB(dbName).drop();
    }

    /**
     * 查找对象 - 根据主键_id
     *
     * @param id
     * @return
     */
    public Document findById(MongoCollection<Document> coll, String id) {
        ObjectId _idobj = null;
        try {
            _idobj = new ObjectId(id);
        } catch (Exception e) {
            return null;
        }
        Document myDoc = coll.find(Filters.eq("_id", _idobj)).first();
        return myDoc;
    }

    /**
     * 统计数
     */
    public int getCount(MongoCollection<Document> coll) {
        int count = (int) coll.count();
        return count;
    }

    /**
     * 条件查询
     */
    public MongoCursor<Document> find(MongoCollection<Document> coll, Bson filter) {
        return coll.find(filter).iterator();
    }

    /**
     * 分页查询
     */
    public MongoCursor<Document> findByPage(MongoCollection<Document> coll, Bson filter, int pageNo, int pageSize) {
        Bson orderBy = new BasicDBObject("_id", 1);
        return coll.find(filter).sort(orderBy).skip((pageNo - 1) * pageSize).limit(pageSize * pageSize).iterator();
    }


    /**
     * 通过ID删除
     *
     * @param coll
     * @param id
     * @return
     */
    public int deleteById(MongoCollection<Document> coll, String id) {
        int count = 0;
        ObjectId _id = null;
        try {
            _id = new ObjectId(id);
        } catch (Exception e) {
            return 0;
        }
        Bson filter = Filters.eq("_id", _id);
        DeleteResult deleteResult = coll.deleteOne(filter);
        count = (int) deleteResult.getDeletedCount();
        return count;
    }

    /**
     * FIXME
     *
     * @param coll
     * @param id
     * @param newdoc
     * @return
     */
    public Document updateById(MongoCollection<Document> coll, String id, Document newdoc) {
        ObjectId _idobj = null;
        try {
            _idobj = new ObjectId(id);
        } catch (Exception e) {
            return null;
        }
        Bson filter = Filters.eq("_id", _idobj);
        // coll.replaceOne(filter, newdoc); // 完全替代
        coll.updateOne(filter, new Document("$set", newdoc));
        return newdoc;
    }

    public void dropCollection(String dbName, String collName) {
        getDB(dbName).getCollection(collName).drop();
    }

    /**
     * 关闭Mongodb
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }

    /**
     * 测试入口
     *
     * @param args
     * @throws CloneNotSupportedException
     */
    public static void main(String[] args) {
        MongoCollection<Document> coll = MongoDBUtil.instance.getDB("webmagic").getCollection("IT");
        System.out.println("******查询总数********");
        System.out.println(coll.count());
        System.out.println("*******查询第一个*******");
        Document first = coll.find().first();
        System.out.println(first.toJson());
        System.out.println("******遍历1********");
        MongoCursor<Document> cursor = coll.find().iterator();
        while (cursor.hasNext()) {
            Document next = cursor.next();
            System.out.println(next.toJson());
        }
        System.out.println("*******遍历2*******");
        for (Document doc : coll.find()) {
            System.out.println(doc.toJson());
        }
        System.out.println("******根据id查询********");
        Document specialId = coll.find(Filters.eq("_id",new ObjectId("5a659bd08fda19297c60803c"))).first();
        System.out.println(specialId.toJson());
        System.out.println("********根据指定字段查询******");
        for (Document s : coll.find(new Document("itHomeArticle.editor","骑士"))) {
            System.out.println(s.toJson());
        }
        System.out.println("********根据指定字段查询并排序******");
        for (Document s :coll.find().sort(Sorts.orderBy(new BasicDBObject("title",-1)))) {
            System.out.println(s.toJson());
        }
        System.out.println("********根据指定字段查询并排序******");
        for (Document s :coll.find().sort(Sorts.ascending("title"))) {
            System.out.println(s.toJson());
        }
        System.out.println("********查询in******");
        for (Document s : coll.find(Filters.in("itHomeArticle.editor",Arrays.asList("飞龙","骑士")))) {
            System.out.println(s.toJson());
        }
        System.out.println("********查询projection******");
        for (Document s : coll.find().projection(Projections.exclude("_id"))) {
            System.out.println(s.toJson());
        }
        System.out.println("********查询Aggregates******");
//        Document agg = coll.aggregate(Arrays.asList(Aggregates.group(null, Accumulators.sum("total", "$i")))).first();
//        System.out.println(agg.toJson());
//        coll.updateOne();
//        coll.deleteOne();
//        coll.insertOne();

        //JSONObject object = JSON.parseObject("{\"itHomeArticle\":{\"author\":\"刺客\",\"comments\":[{\"comment\":\"把招工说的这么清新脱俗\",\"floor\":\"1楼\",\"mobile\":\"红米 Note 4X\",\"nick\":\"叫嘛名好啊\",\"pos\":\"IT之家山东网友\",\"time\":\"2017-12-1 15:38:30\",\"userId\":\"1176407\"},{\"comment\":\"应聘\",\"floor\":\"2楼\",\"mobile\":\"iPhone 6 金\",\"nick\":\"请叫我校长\",\"pos\":\"IT之家安徽六安网友\",\"time\":\"2017-12-1 15:38:31\",\"userId\":\"1101090\"},{\"comment\":\"刺客很快来了，刺客你到了吗，刺客老大好，喝茶～\",\"floor\":\"3楼\",\"mobile\":\"举报\",\"nick\":\"我欲乘风春楼\",\"pos\":\"IT之家浙江温州网友\",\"time\":\"2017-12-1 15:38:45\",\"userId\":\"1546147\"},{\"comment\":\"为神马没有游戏编辑？我开车比白猫稳多了\",\"floor\":\"4楼\",\"mobile\":\"魅蓝 metal\",\"nick\":\"敬微软服谷歌爱苹果\",\"pos\":\"IT之家广西柳州网友\",\"time\":\"2017-12-1 15:39:05\",\"userId\":\"807490\"},{\"comment\":\"能为刺客适当挡酒......\",\"floor\":\"5楼\",\"mobile\":\"举报\",\"nick\":\"大到暴雨\",\"pos\":\"IT之家广东深圳网友\",\"time\":\"2017-12-1 15:39:29\",\"userId\":\"816975\"},{\"comment\":\"芒果羊就是这样进去的吗\uD83C\uDF38\uD83D\uDC14\",\"floor\":\"6楼\",\"mobile\":\"诺基亚 6（2017）\",\"nick\":\"腾讯QQ兼容性测试人员\",\"pos\":\"IT之家湖北网友\",\"time\":\"2017-12-1 15:39:45\",\"userId\":\"1342373\"},{\"comment\":\"一楼\",\"floor\":\"7楼\",\"mobile\":\"iPhone 6s 金\",\"nick\":\"渣渣男\",\"pos\":\"IT之家湖北网友\",\"time\":\"2017-12-1 15:39:54\",\"userId\":\"1493834\"},{\"comment\":\"让我想到了守望宣传片，你们来不来？不来\",\"floor\"c:\"8楼\",\"mobile\":\"举报\",\"nick\":\"苏米喵\",\"pos\":\"IT之家浙江温州网友\",\"time\":\"2017-12-1 15:39:55\",\"userId\":\"1496388\"},{\"comment\":\"有没有招it直播的，扫地的，路过\",\"floor\":\"9楼\",\"mobile\":\"举报\",\"nick\":\"我欲乘风春楼\",\"pos\":\"IT之家浙江温州网友\",\"time\":\"2017-12-1 15:39:56\",\"userId\":\"1546147\"},{\"comment\":\"路过。。\",\"floor\":\"10楼\",\"mobile\":\"三星 Galaxy S6 Active\",\"nick\":\"手机用户2391180355\",\"pos\":\"IT之家重庆网友\",\"time\":\"2017-12-1 15:40:25\",\"userId\":\"401015\"},{\"comment\":\"真么多要求我只能做到“为刺客适当挡酒”\uD83C\uDF39\uD83D\uDC23\",\"floor\":\"11楼\",\"mobile\":\"红米 Note 3\",\"nick\":\"容易坏提\",\"pos\":\"IT之家河南网友\",\"time\":\"2017-12-1 15:40:27\",\"userId\":\"1479860\"},{\"comment\":\"请问。。。。招不招扫地的？？\",\"floor\":\"12楼\",\"mobile\":\"魅族 PRO 6 Plus\",\"nick\":\"前排正面\",\"pos\":\"IT之家西藏拉萨网友\",\"time\":\"2017-12-1 15:40:38\",\"userId\":\"1218031\"},{\"comment\":\"好想跟着刺客大大混，他说学历什么的都是历史，我想问问，我在工地上打杂，我很想学，可以来吗？\",\"floor\":\"13楼\",\"mobile\":\"iPhone 6 金\",\"nick\":\"冰点水\",\"pos\":\"IT之家重庆网友\",\"time\":\"2017-12-1 15:40:39\",\"userId\":\"854551\"},{\"comment\":\"今晚去之家有鸡腿加吗？\",\"floor\":\"14楼\",\"mobile\":\"红米 Note 4\",\"nick\":\"时间让自己改变\",\"pos\":\"IT之家广西网友\",\"time\":\"2017-12-1 15:41:01\",\"userId\":\"1260040\"},{\"comment\":\"看到开放这么多岗位，我才感觉到，刺客真的需要我\",\"floor\":\"15楼\",\"mobile\":\"举报\",\"nick\":\"万山\",\"pos\":\"IT之家北京网友\",\"time\":\"2017-12-1 15:41:16\",\"userId\":\"1159406\"},{\"comment\":\"挤一挤\",\"floor\":\"16楼\",\"mobile\":\"举报\",\"nick\":\"人形铺路机\",\"pos\":\"IT之家黑龙江网友\",\"time\":\"2017-12-1 15:41:26\",\"userId\":\"1433912\"},{\"comment\":\"需要水管工吗？毕竟之家那么多水。\",\"floor\":\"17楼\",\"mobile\":\"举报\",\"nick\":\"绿茵守望者\",\"pos\":\"IT之家浙江宁波网友\",\"time\":\"2017-12-1 15:41:31\",\"userId\":\"224433\"},{\"comment\":\"依然不写薪资待遇。。。\",\"floor\":\"18楼\",\"mobile\":\"举报\",\"nick\":\"C4Designer\",\"pos\":\"IT之家上海网友\",\"time\":\"2017-12-1 15:41:37\",\"userId\":\"808519\"},{\"comment\":\"待遇面议。\",\"floor\":\"19楼\",\"mobile\":\"三星 Galaxy S7\",\"nick\":\"芬兰某军工厂执行董事\",\"pos\":\"IT之家江苏南京网友\",\"time\":\"2017-12-1 15:42:10\",\"userId\":\"1211562\"},{\"comment\":\"会撩妹的要不要\",\"floor\":\"20楼\",\"mobile\":\"360手机 N4S\",\"nick\":\"我要买surface\",\"pos\":\"IT之家安徽网友\",\"time\":\"2017-12-1 15:42:20\",\"userId\":\"1416243\"},{\"comment\":\"请问招不招专科的\uD83D\uDE0F\uD83D\uDE0F\uD83D\uDE0F\",\"floor\":\"21楼\",\"mobile\":\"小米 5\",\"nick\":\"厅长_祁同伟\",\"pos\":\"IT之家四川网友\",\"time\":\"2017-12-1 15:42:35\",\"userId\":\"1296336\"},{\"comment\":\"恕我直言，我把这个世界很痛快，一生自由做最爱看成了这个世界很痛快，一生自由最做*。\",\"floor\":\"22楼\",\"mobile\":\"红米 Note 4X\",\"nick\":\"vluoke\",\"pos\":\"IT之家湖南网友\",\"time\":\"2017-12-1 15:42:37\",\"userId\":\"1250980\"},{\"comment\":\"挺好的 \",\"floor\":\"23楼\",\"mobile\":\"iPhone 5s 深空灰\",\"nick\":\"穆拉o_O\",\"pos\":\"IT之家广东佛山网友\",\"time\":\"2017-12-1 15:42:39\",\"userId\":\"1598342\"},{\"comment\":\"面基，不错与刺客举酒望明月\",\"floor\":\"24楼\",\"mobile\":\"举报\",\"nick\":\"我欲乘风春楼\",\"pos\":\"IT之家浙江温州网友\",\"time\":\"2017-12-1 15:42:53\",\"userId\":\"1546147\"},{\"comment\":\"6666666啊\",\"floor\":\"25楼\",\"mobile\":\"一加手机 5\",\"nick\":\"鱼哥大人\",\"pos\":\"IT之家江苏网友\",\"time\":\"2017-12-1 15:43:20\",\"userId\":\"1283895\"},{\"comment\":\"微博运营？\",\"floor\":\"26楼\",\"mobile\":\"举报\",\"nick\":\"人形铺路机\",\"pos\":\"IT之家黑龙江网友\",\"time\":\"2017-12-1 15:43:25\",\"userId\":\"1433912\"},{\"comment\":\"初中毕业要吗\",\"floor\":\"27楼\",\"mobile\":\"iPhone 8 银\",\"nick\":\"黄志松\",\"pos\":\"IT之家福建网友\",\"time\":\"2017-12-1 15:43:34\",\"userId\":\"908747\"},{\"comment\":\"没有蓝莲，一切免谈\uD83C\uDF38\uD83D\uDC14\",\"floor\":\"28楼\",\"mobile\":\"iPhone X 银\",\"nick\":\"専念千年\",\"pos\":\"IT之家江苏无锡网友\",\"time\":\"2017-12-1 15:43:34\",\"userId\":\"1095943\"},{\"comment\":\"学物流的大四学生，现在正在一家第三方物流实习，如果可以的话，随时愿意加入咱们团队，销售也不错\",\"floor\":\"29楼\",\"mobile\":\"iPhone 7 金\",\"nick\":\"我s2you\",\"pos\":\"IT之家北京网友\",\"time\":\"2017-12-1 15:43:36\",\"userId\":\"854251\"},{\"comment\":\"本人精通各种APP下载安装和卸载...\",\"floor\":\"30楼\",\"mobile\":\"小米 MIX 2\",\"nick\":\"环院大婊哥\",\"pos\":\"IT之家广东汕头网友\",\"time\":\"2017-12-1 15:43:39\",\"userId\":\"1378427\"},{\"comment\":\"卡吧，图吧，wp7吧11级要吗？\",\"floor\":\"31楼\",\"mobile\":\"一加手机 3\",\"nick\":\"神官阁月\",\"pos\":\"IT之家江西网友\",\"time\":\"2017-12-1 15:43:39\",\"userId\":\"1043303\"},{\"comment\":\"我是来看哪个老段子的，好像还没啊……？\uD83D\uDE02\",\"floor\":\"32楼\",\"mobile\":\"红米 4\",\"nick\":\"一阴一阳之谓道\",\"pos\":\"IT之家重庆网友\",\"time\":\"2017-12-1 15:43:40\",\"userId\":\"1080805\"},{\"comment\":\"还招 uwp开发啊\",\"floor\":\"33楼\",\"mobile\":\"举报\",\"nick\":\"波音绿化公司\",\"pos\":\"IT之家吉林长春网友\",\"time\":\"2017-12-1 15:43:47\",\"userId\":\"1030467\"},{\"comment\":\"待遇面议不知道有没有1万\",\"floor\":\"34楼\",\"mobile\":\"iPhone 6s 金\",\"nick\":\"吾愛萝莉\",\"pos\":\"IT之家陕西西安网友\",\"time\":\"2017-12-1 15:43:50\",\"userId\":\"74809\"},{\"comment\":\"需要端茶的吗\",\"floor\":\"35楼\",\"mobile\":\"一加手机 5\",\"nick\":\"鱼哥大人\",\"pos\":\"IT之家江苏网友\",\"time\":\"2017-12-1 15:43:51\",\"userId\":\"1283895\"},{\"comment\":\"多少钱？\",\"floor\":\"36楼\",\"mobile\":\"iPhone 6s 银\",\"nick\":\"隔壁老陈\",\"pos\":\"IT之家广东东莞网友\",\"time\":\"2017-12-1 15:44:01\",\"userId\":\"113971\"},{\"comment\":\"全方面发展了吗\uD83E\uDD14\",\"floor\":\"37楼\",\"mobile\":\"索尼 Xperia Z5 尊享版\",\"nick\":\"买镜子送的_\",\"pos\":\"IT之家广东网友\",\"time\":\"2017-12-1 15:44:11\",\"userId\":\"1377819\"},{\"comment\":\"坐等会开机那个段子\",\"floor\":\"40楼\",\"mobile\":\"小米 6\",\"nick\":\"wp咸蛋\",\"pos\":\"IT之家河北承德网友\",\"time\":\"2017-12-1 15:44:14\",\"userId\":\"789770\"},{\"comment\":\"我要和玄隐一起铺路\",\"floor\":\"41楼\",\"mobile\":\"HTC U11\",\"nick\":\"纯洁的单眼皮\",\"pos\":\"IT之家河南网友\",\"time\":\"2017-12-1 15:44:14\",\"userId\":\"939884\"},{\"comment\":\"什么都不会明年毕业可以投简历吗\",\"floor\":\"42楼\",\"mobile\":\"华为 P9\",\"nick\":\"撞了个寂寞\",\"pos\":\"IT之家安徽网友\",\"time\":\"2017-12-1 15:44:43\",\"userId\":\"1316206\"},{\"comment\":\"我要来Android\",\"floor\":\"43楼\",\"mobile\":\"魅族 Pro 5\",\"nick\":\"Kikyou_\",\"pos\":\"IT之家北京网友\",\"time\":\"2017-12-1 15:45:02\",\"userId\":\"1414441\"},{\"comment\":\"高中毕业，待业五年，想去一试\",\"floor\":\"44楼\",\"mobile\":\"iPhone X 深空灰\",\"nick\":\"放羊小胖\",\"pos\":\"IT之家广东广州网友\",\"time\":\"2017-12-1 15:45:06\",\"userId\":\"880118\"},{\"comment\":\"有驾照，能为刺客适当挡酒，可出差。酒驾？\",\"floor\":\"45楼\",\"mobile\":\"举报\",\"nick\":\"人形铺路机\",\"pos\":\"IT之家黑龙江网友\",\"time\":\"2017-12-1 15:45:08\",\"userId\":\"1433912\"},{\"comment\":\"没有铺路部？这不科学！更不之家！！\",\"floor\":\"46楼\",\"mobile\":\"举报\",\"nick\":\"UC震惊部首席发言人\",\"pos\":\"IT之家广西网友\",\"time\":\"2017-12-1 15:45:13\",\"userId\":\"1533091\"},{\"comment\":\"滑稽\",\"floor\":\"47楼\",\"mobile\":\"小米 MIX 2\",\"nick\":\"云往昔\",\"pos\":\"IT之家陕西网友\",\"time\":\"2017-12-1 15:45:14\",\"userId\":\"1207703\"},{\"comment\":\"刺客威武，一统江湖。\",\"floor\":\"48楼\",\"mobile\":\"三星 Galaxy S8+\",\"nick\":\"灰豆大仙\",\"pos\":\"IT之家内蒙古呼和浩特网友\",\"time\":\"2017-12-1 15:45:15\",\"userId\":\"1265316\"},{\"comment\":\"应届的可以么\",\"floor\":\"49楼\",\"mobile\":\"红米 Note 4X\",\"nick\":\"啥呢\",\"pos\":\"IT之家山东网友\",\"time\":\"2017-12-1 15:45:32\",\"userId\":\"1065851\"},{\"comment\":\"震惊部缺人吗\uD83D\uDE33\",\"floor\":\"50楼\",\"mobile\":\"iPhone SE 深空灰\",\"nick\":\"梦未散\",\"pos\":\"IT之家江苏苏州网友\",\"time\":\"2017-12-1 15:45:35\",\"userId\":\"1241496\"},{\"comment\":\"文笔好，能水爱水会水享受水\",\"floor\":\"51楼\",\"mobile\":\"HTC U11\",\"nick\":\"liop\",\"pos\":\"IT之家福建厦门网友\",\"time\":\"2017-12-1 15:45:36\",\"userId\":\"834704\"},{\"comment\":\"First first\",\"floor\":\"52楼\",\"mobile\":\"Apple Watch\",\"nick\":\"Wiseme06\",\"pos\":\"IT之家上海网友\",\"time\":\"2017-12-1 15:45:48\",\"userId\":\"1244649\"}],\"content\":\"\\n原本想一如既往地，想起个诗意些的标题做下招聘，但是想想这次有些特殊，还是用更直白的表达吧。\\n\\n十年一剑，2018年，IT之家、辣品新业务部门将拆分独立，并完成合伙人激励体系\\n，对于不久的未来，奠定一个更好的飞天基础。我们坚信，IT之家和电商等业务将迎来一个厚积薄发的突破期，高速成长将需要更优秀的人才和先进合理的人才激励体系。\\n\\n\\n我们这次停掉外部所有招聘网站上的岗位投放，也将只从软媒的老朋友里面挑选我们今后的队友伙伴，我们有信心地向大家保证公平合理的回报会超越北上广深，我们确定的是，那句戏谑的被修改的广告语“农妇、山泉、有点田”不会离你很远。“这个世界不痛快，操劳一生为房贷”将变为：“这个世界很痛快，一生自由做最爱”。在全国最宜居之一的城市里，你能找到一群志同道合的伙伴，一起通过创造正向社会价值，获取合伙人荣耀红利双回报——一起努力，一起创造荣耀和财富，一起分享，一起向前……\\n\\n在浏览岗位之前，感兴趣的同学请务必看下3原则和招聘注意事项，同时，对比过往，这次招聘有程序员和摄影专家等一些新岗位，请大家留意。\\n\\n一、“媒人”录取的基础3原则\\n\\n * \\n价值趋同：认同软媒的价值观，即“存在 - 创造价值”，个体和团队的存在，一定要为人类社会和中国社会创造更多的正向价值，这是第一位的；\\n\\n * \\n品性第二：厚道、本分、诚实、谦逊、坦诚、热心、协作；\\n\\n * \\n学习能力：想学，会学，主动学——学历高低和过往历史证明过去，学习能力是真正的进步阶梯；\\n\\n二、简历投递和此次招聘的注意事项\\n\\n\\n * \\n投递简历时邮件主题里请注明岗位，并务必附上简历及作品或成绩（如有），邮件附件或者网址均可以；\\n\\n * \\n所有岗位均为全职，工作地点现阶段位于山东省青岛市市南区动漫园E座；\\n\\n * \\n所有岗位我们将培养和优选合伙人，激励成长\\n\\n\\n\\n三、本次招聘岗位和具体要求\\n\\nA、编辑部：网站编辑/主编 \\n\\n文笔好，能写爱写会写享受写，对行业热爱，对未来憧憬。\\n\\n * \\n汽车版主编：文笔好，爱车，懂车，尤其关注高科技和新能源汽车\\n\\n * \\n摄影编辑：懂大炮小炮，懂光懂灯，懂摄影摄像之艺术，懂美，有好作品\\n\\n\\n * \\n家电编辑：黑电白电小家电，智能家庭套路明\\n\\n * \\nDIY 编辑：懂硬道理，实力就靠玩硬的\\n\\n * \\n学院编辑：学院派，玩转Windows，玩转iOS，玩转安卓，精通科技产品秘笈大法\\n\\n\\n * \\n评测编辑：爱数码爱未来，能折腾会折腾，懂产品懂厂商懂行业\\n\\n * \\n快讯编辑：发现快，响应快，动作快，推送快 —— 唯快不破\\n\\n * \\n评论编辑：针对热点焦点，解析剖析痛点亮点，让读者痛快舒爽酣畅淋漓\\n\\n * \\n导购编辑：买东西听你的准没错，给大家选出真正的好东西，不仅仅划算，而是最实用最好用\\n\\n * \\n外翻编辑：专8只是起点，你还爱科技，爱新鲜\\n\\nB、新媒体内容运营\\n\\n\\n * \\n微信运营：不做最好毋宁死，浓缩最焦点成精华，10万+只是入门\\n\\n * \\n微博运营：深懂互动为众妙之门\\n\\nC、产品部：开发/设计\\n\\n软媒的产品两大基本需求：性能、稳定，这是一切开发工作的起点和基础准则。\\n\\n * \\n.NET 网站开发：高手，高手，高手！\\n\\n * \\nApp开发：iOS/安卓/UWP三个开发方向，有大型App开发经验，一流水准顶级技术；\\n\\n * \\n设计/美工：有手绘基础，美感卓然，面子的事就拜托了\\n\\n\\nC、业务部：广告销售/媒介执行\\n\\n销售的本质是什么？懂人性，善交际。男女均可，身体好，有驾照，能为刺客适当挡酒，可出差。\\n\\n * \\n广告销售：1年以上广告销售经验，拥有良好的广告经营渠道关系及行业资源尤佳，有广告公司公关公司从业经历者尤佳\\n\\n * \\n媒介执行：业务支持、客户需求发掘和维护\\n\\nD、运营（产品运营/市场运营）\\n\\n\\n * \\n运营总监：深懂从0到1之道术器，全面负责各产品运维和市场拓展\\n\\n * \\n市场运营：精通客户端推广或者善于组织有效市场活动，合理组织配置内外部资源达到目标\\n\\n\\n期待合伙人，期待同行！\\n\\n\\n软媒 - 存在，创造价值。\\n\\n刺客，软媒 CEO，青岛，2017年12月1日。\\n\",\"editor\":\"刺客\",\"hotComments\":[{\"comment\":\"本人擅长 Ai、Fw、Fl、Br、Ae、Pr、Id、Ps 等软件的安装与卸载，精通 CSS、JavaScript、PHP、ASP、C、C++、C#、Java、Ruby、Perl、Lisp、Python、Objective-C、ActionScript、Pascal 等单词的拼写，熟悉 Windows、Linux、OS X、Android、iOS、WP8 等系统的开关机。\",\"floor\":\"73楼\",\"mobile\":\"魅蓝 Note 6\",\"nick\":\"锻炼丁丁保卫祖国\",\"pos\":\"IT之家湖南网友\",\"time\":\"2017-12-1 15:46:56\",\"userId\":\"1318179\"},{\"comment\":\"预感到了这条内容的出现，在之家绝对是必然 →_→\",\"floor\":\"73楼8#\",\"mobile\":\"旗鱼浏览器\",\"nick\":\"刺客\",\"pos\":\"IT之家山东青岛网友\",\"time\":\"2017-12-1 15:50:28\",\"userId\":\"1\"},{\"comment\":\"这篇招聘文章写的还是很有水平的，开头就给你了希望，让你觉得自己还是有价值的，然后又说能力强弱没有关系，只要有上进心一切都来得及，最后招聘信息只字未提学历，但是看完岗位要求你自己已经知难而退了。丝毫没有明面上对你自尊心的打击，甚至还在估计你，实则已经隐晦说明，要认清自己，努力上进，以后还是有机会的。很好 很好！\",\"floor\":\"165楼\",\"mobile\":\"努比亚 Z9 Max\",\"nick\":\"ID845556\",\"pos\":\"IT之家陕西西安网友\",\"time\":\"2017-12-1 16:01:11\",\"userId\":\"845556\"},{\"comment\":\"某些Linux的开关机你真不见得会\",\"floor\":\"73楼6#\",\"mobile\":\"iPhone 7 黑\",\"nick\":\"玩世不恭808\",\"pos\":\"IT之家福建漳州网友\",\"time\":\"2017-12-1 15:49:31\",\"userId\":\"1162531\"},{\"comment\":\"本人擅长Ai、Fw、Fl、Br、Ae、Pr、Id、Ps等软件的安装与卸载，精通CSS、JavaScript、PHP、ASP、C、C＋＋、C#、Java、Ruby、Perl、Lisp、python、Objective-C、ActionScript、Pascal、spss、sas等单词的拼写，熟悉Windows、Linux、Mac、Android、IOS、WM10等系统的开关机，熟悉Word文档的打开与关闭、精通文章的复制与粘贴、曾在人人影视下载过各种美剧与电影的字幕、多年翻墙出学校去网吧通宵上网风雨无阻、非常熟悉西方的那一套理论，与中国移动、中国国石油等500强企业有密切的业务联系，本人与马云、马化腾三人的资产总和足以撼动亚洲经济，每年定期参加总金额高达千亿的双十一电子商务活动。请问IT之家需要我这样的人才吗，急，在线等。\",\"floor\":\"133楼\",\"mobile\":\"魅族 Pro 7 高配版\",\"nick\":\"约瑟夫_斯大林\",\"pos\":\"IT之家浙江网友\",\"time\":\"2017-12-1 15:52:35\",\"userId\":\"1400117\"},{\"comment\":\"来吧(//∇//)\",\"floor\":\"207楼1#\",\"mobile\":\"iPhone SE 深空灰\",\"nick\":\"文轩\",\"pos\":\"IT之家山东青岛网友\",\"time\":\"2017-12-1 16:16:52\",\"userId\":\"1472493\"},{\"comment\":\"一锤子下去不就关了\",\"floor\":\"73楼7#\",\"mobile\":\"iPhone 8 Plus 深空灰\",\"nick\":\"取名字真的难\",\"pos\":\"IT之家广东佛山网友\",\"time\":\"2017-12-1 15:50:13\",\"userId\":\"1478008\"},{\"comment\":\"原来冠军被你内定了！\",\"floor\":\"73楼2#\",\"mobile\":\"三星 Galaxy Note 8\",\"nick\":\"流年慕染\",\"pos\":\"IT之家广西网友\",\"time\":\"2017-12-1 15:49:06\",\"userId\":\"1580181\"}],\"id\":\"336817\",\"imgLinks\":[\"https://img.ithome.com/newsuploadfiles/2017/12/20171201_153127_648.png\"],\"source\":\"IT之家\",\"time\":\"2017-12-1 15:37:33\",\"title\":\"IT之家合伙人，招聘！\"},\"title\":\"IT之家合伙人，招聘！\"}");
        //Document doc = new Document(object);

        //coll.insertOne(doc);

//        MongoDBUtil.instance.getDB("webmagic").createCollection("IT");
//        MongoDBUtil.instance.getDB("webmagic").getCollection("IT").insertOne();
//        //1.getCollection
//        MongoCollection<Document> collection = MongoDBUtil.instance.getCollection("db","col");
//        collection.insertOne(new Document(""));
//        FindIterable<Document> inter = collection.find(new Document("title","PHP 教程")).sort(Sorts.ascending("title"));
//        for (Document doc : inter) {
//            System.out.println(doc.toJson());
//        }
//
//
//        List<String> list = MongoDBUtil.instance.getAllCollections("db");
//        for (String s : list) {
//            System.out.println(s);
//        }


//        String dbName = "test";
//        String collName = "wd_paper_scie";
//        MongoCollection<Document> coll = MongoDBUtil.instance.getCollection(dbName, collName);
        //coll.createIndex(new Document("validata",1));//创建索引
        //coll.createIndex(new Document("id",1));
        // coll.createIndex(new Document("ut_wos",1),new IndexOptions().unique(true));//创建唯一索引
        //coll.dropIndexes();//删除索引
        //coll.dropIndex("validata_1");//根据索引名删除某个索引
//        ListIndexesIterable<Document> list = coll.listIndexes();//查询所有索引
//        for (Document document : list) {
//            System.out.println(document.toJson());
//        }
//        coll.find(Filters.and(Filters.eq("x", 1), Filters.lt("y", 3)));
//        coll.find(Filters.and(Filters.eq("x", 1), lt("y", 3)));
//        coll.find().sort(Sorts.ascending("title"));
//        coll.find().sort(new Document("id",1));
//        coll.find(new Document("$or", Arrays.asList(new Document("owner", "tom"), new Document("words", new Document("$gt", 350)))));
//        coll.find().projection(fields(include("title", "owner"), excludeId()));
        // coll.updateMany(Filters.eq("validata", 1), Updates.set("validata", 0));
        //coll.updateMany(Filters.eq("validata", 1), new Document("$unset", new Document("jigou", "")));//删除某个字段
        //coll.updateMany(Filters.eq("validata", 1), new Document("$rename", new Document("affiliation", "affiliation_full")));//修改某个字段名
        //coll.updateMany(Filters.eq("validata", 1), new Document("$rename", new Document("affiliationMeta", "affiliation")));
        //coll.updateMany(Filters.eq("validata", 1), new Document("$set", new Document("validata", 0)));//修改字段值
//        MongoCursor<Document> cursor1 =coll.find(Filters.eq("ut_wos", "WOS:000382970200003")).iterator();
//        while(cursor1.hasNext()){
//            Document sd=cursor1.next();
//            System.out.println(sd.toJson());
//            coll.insertOne(sd);
//        }

//        MongoCursor<Document> cursor1 =coll.find(Filters.elemMatch("affInfo", Filters.eq("firstorg", 1))).iterator();
//        while(cursor1.hasNext()){
//            Document sd=cursor1.next();
//            System.out.println(sd.toJson());
//        }
        //查询只返回指定字段
        // MongoCursor<Document> doc= coll.find().projection(Projections.fields(Projections.include("ut_wos","affiliation"),Projections.excludeId())).iterator();
        //"ut_wos" : "WOS:000382970200003"
        //coll.updateMany(Filters.eq("validata", 1), new Document("$set", new Document("validata", 0)));
        //coll.updateMany(Filters.eq("validata", 0), new Document("$rename", new Document("sid", "ssid")), new UpdateOptions().upsert(false));
        //coll.updateOne(Filters.eq("ut_wos", "WOS:000382970200003"), new Document("$set", new Document("validata", 0)));
        //long isd=coll.count(Filters.elemMatch("affInfo", Filters.elemMatch("affiliationJGList", Filters.eq("sid", 0))));
        // System.out.println(isd);
        //MongoCursor<Document> doc= coll.find(Filters.elemMatch("affInfo", Filters.elemMatch("affiliationJGList", Filters.ne("sid", 0)))).projection(Projections.fields(Projections.elemMatch("affInfo"),Projections.excludeId())).iterator();
//       MongoCursor<Document> doc= coll.find().projection(Projections.include("affInfo","ssid")).iterator();
//       while(doc.hasNext()){
//            Document sd=doc.next();
//            System.out.println(sd.toJson());
//
//        }
//        MongoDBUtil.instance.close();
        // 插入多条
//         for (int i = 1; i <= 4; i++) {
//         Document doc = new Document();
//         doc.put("name", "zhoulf");
//         doc.put("school", "NEFU" + i);
//         Document interests = new Document();
//         interests.put("game", "game" + i);
//         interests.put("ball", "ball" + i);
//         doc.put("interests", interests);
//         coll.insertOne(doc);
//         }
//
       /* MongoCursor<Document> sd =coll.find().iterator();
        while(sd.hasNext()){
            Document doc = sd.next();
            String id =  doc.get("_id").toString();
            List<AffiliationJG> list = new ArrayList<AffiliationJG>();
            AffiliationJG jg = new AffiliationJG();
            jg.setAddress("123");
            jg.setCid(2);
            jg.setCname("eeee");
            jg.setSid(3);
            jg.setSname("rrrr");
            AffiliationJG jg2 = new AffiliationJG();
            jg2.setAddress("3242");
            jg2.setCid(2);
            jg2.setCname("ers");
            jg2.setSid(3);
            jg2.setSname("rasdr");
            list.add(jg);
            list.add(jg2);
            AffiliationList af = new AffiliationList();
            af.setAffiliationAuthos("33333");
            af.setAffiliationinfo("asdsa");
            af.setAffiliationJGList(list);
            JSONObject json = JSONObject.fromObject(af);
            doc.put("affInfo", json);
            MongoDBUtil.instance.updateById(coll, id, doc);
        }*/

//        Bson bs = Filters.eq("name", "zhoulf");
//        coll.find(bs).iterator();
        // // 根据ID查询
        // String id = "556925f34711371df0ddfd4b";
        // Document doc = MongoDBUtil2.instance.findById(coll, id);
        // System.out.println(doc);

        // 查询多个
        // MongoCursor<Document> cursor1 = coll.find(Filters.eq("name", "zhoulf")).iterator();
        // while (cursor1.hasNext()) {
        // org.bson.Document _doc = (Document) cursor1.next();
        // System.out.println(_doc.toString());
        // }
        // cursor1.close();

        // 查询多个
//         MongoCursor<WdPaper> cursor2 = coll.find(WdPaper.class).iterator();
//         while(cursor2.hasNext()){
//             WdPaper doc = cursor2.next();
//             System.out.println(doc.getUt_wos());
//         }
        // 删除数据库
        // MongoDBUtil2.instance.dropDB("testdb");

        // 删除表
        // MongoDBUtil2.instance.dropCollection(dbName, collName);

        // 修改数据
        // String id = "556949504711371c60601b5a";
        // Document newdoc = new Document();
        // newdoc.put("name", "时候");
        // MongoDBUtil.instance.updateById(coll, id, newdoc);

        // 统计表
        //System.out.println(MongoDBUtil.instance.getCount(coll));

        // 查询所有
//        Bson filter = Filters.eq("count", 0);
//        MongoDBUtil.instance.find(coll, filter);

    }
}

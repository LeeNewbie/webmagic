package us.codecraft.webmagic.pipeline;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.util.MongoDBUtil;

/**
 * @package: us.codecraft.webmagic.pipeline
 * @ClassName: MongodbPipeline
 * @Description:
 * @author: liting
 * @date: 2018-01-22 11:51
 */
public class MongodbPipeline implements Pipeline {

    private String dBName = "webmagic";
    private String collection;

    public MongodbPipeline(String dBName, String collection) {
        this.dBName = dBName;
        this.collection = collection;
    }

    public MongodbPipeline(String collection) {
        this.collection = collection;
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        if (resultItems.getAll().size() == 0) {
            return;
        }
        MongoCollection<Document> itColl =  MongoDBUtil.instance.getDB(dBName).getCollection(collection);
        itColl.insertOne(new Document(JSONObject.parseObject(JSON.toJSONString(resultItems.getAll()))));
    }
}

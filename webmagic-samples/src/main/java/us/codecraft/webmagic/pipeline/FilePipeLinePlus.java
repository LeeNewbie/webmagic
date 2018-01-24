package us.codecraft.webmagic.pipeline;

import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.annotation.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.selector.Json;
import us.codecraft.webmagic.utils.FilePersistentBase;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;

/**
 * @package: us.codecraft.webmagic.pipeline
 * @ClassName: FilePipeLinePlus
 * @Description:
 * @author: liting
 * @date: 2018-01-18 13:44
 */
@ThreadSafe
public class FilePipeLinePlus extends FilePersistentBase implements Pipeline{
    private Logger logger = LoggerFactory.getLogger(getClass());

    public static final String RESULT_ITEM_KEY_TITLE = "title";
    /**
     * create a FilePipeline with default path"/data/webmagic/"
     */
    public FilePipeLinePlus() {
        setPath("/data/webmagic/");
    }

    public FilePipeLinePlus(String path) {
        setPath(path);
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        String path = this.path + task.getUUID() + PATH_SEPERATOR;
        PrintWriter printWriter = null;
        try {
            if (resultItems.get(RESULT_ITEM_KEY_TITLE) != null) {
                printWriter = new PrintWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(
                                        getFile(path
                                                + resultItems.get(RESULT_ITEM_KEY_TITLE).toString().replace(PATH_SEPERATOR,"")
                                                + ".json"))
                                , "UTF-8"));

            } else {
//                printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(getFile(path + DigestUtils.md5Hex(resultItems.getRequest().getUrl()) + ".html")),"UTF-8"));
                return;
            }
//            printWriter.println("url:\t" + resultItems.getRequest().getUrl());
            printWriter.println(JSON.toJSON(resultItems.getAll()));
        } catch (IOException e) {
            logger.warn("write file error", e);
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }
}

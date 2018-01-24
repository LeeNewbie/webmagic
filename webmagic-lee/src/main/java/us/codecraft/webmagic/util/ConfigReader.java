package us.codecraft.webmagic.util;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Created by Administrator on 2017/7/10.
 */
public class ConfigReader {

    private static PropertiesConfiguration configuration = new PropertiesConfiguration();

    static{
        try {
            configuration.load(ConfigReader.class.getResourceAsStream("/redis.properties"));

        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key){
        return configuration.getString(key);
    }
}

package us.codecraft.webmagic.downloader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.ProxyProvider;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.CharsetUtils;
import us.codecraft.webmagic.utils.HttpClientUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


/**
 * The http downloader based on HttpClient.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
@ThreadSafe
public class HttpClientDownloader extends AbstractDownloader {

    private Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 根据主域名-httpclient实体
     */
    private final Map<String, CloseableHttpClient> httpClients = new HashMap<String, CloseableHttpClient>();
    /**
     * httpclient生成器
     */
    private HttpClientGenerator httpClientGenerator = new HttpClientGenerator();
    /**
     * 封装请求转换器
     */
    private HttpUriRequestConverter httpUriRequestConverter = new HttpUriRequestConverter();
    /**
     * 代理ip提供
     */
    private ProxyProvider proxyProvider;
    /**
     * 保留响应头
     */
    private boolean responseHeader = true;
    /**
     * 如果文件存在 则删除后保存
     */
    private boolean fileForceSave = true;

    public static final String BASE_FILE_SAVE_PATH = "D:\\data\\";

    public void setHttpUriRequestConverter(HttpUriRequestConverter httpUriRequestConverter) {
        this.httpUriRequestConverter = httpUriRequestConverter;
    }

    public void setProxyProvider(ProxyProvider proxyProvider) {
        this.proxyProvider = proxyProvider;
    }

    /**
     * 根据域名获取对应的httpclient实例
     *
     * @param site
     * @return
     */
    private CloseableHttpClient getHttpClient(Site site) {

        if (site == null) {
            return httpClientGenerator.getClient(null);
        }
        String domain = site.getDomain();
        CloseableHttpClient httpClient = httpClients.get(domain);
        if (httpClient == null) {
            synchronized (this) {
                httpClient = httpClients.get(domain);
                if (httpClient == null) {
                    httpClient = httpClientGenerator.getClient(site);
                    httpClients.put(domain, httpClient);
                }
            }
        }
        return httpClient;
    }

    /**
     * 下载页面
     *
     * @param request request
     * @param task    task
     * @return Page
     */
    @Override
    public Page download(Request request, Task task) {
        if (task == null || task.getSite() == null) {
            throw new NullPointerException("task or site can not be null");
        }
        CloseableHttpResponse httpResponse = null;
        CloseableHttpClient httpClient = getHttpClient(task.getSite());
        Proxy proxy = proxyProvider != null ? proxyProvider.getProxy(task) : null;
        //转换
        HttpClientRequestContext requestContext = httpUriRequestConverter.convert(request, task.getSite(), proxy);
        Page page = Page.fail();
        try {
            httpResponse = httpClient.execute(requestContext.getHttpUriRequest(), requestContext.getHttpClientContext());
            if (request.isRequestAsAFile()) {
                //下载文件
                page = handleFileResponse(request, task.getSite().getCharset(), httpResponse, task);
            } else {
                //下载页面
                page = handleResponse(request, task.getSite().getCharset(), httpResponse, task);
            }
            // TODO: 2018/1/8 0008  页面下载成功时对请求的处理
            onSuccess(request);
            logger.info("downloading page success {}", request.getUrl());
            return page;
        } catch (IOException e) {
            logger.warn("download page {} error", request.getUrl(), e);
            // TODO: 2018/1/8 0008  页面下载失败时对请 求的处理-例如记录失败次数
            onError(request);
            return page;
        } finally {
            if (httpResponse != null) {
                //ensure the connection is released back to pool
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
            if (proxyProvider != null && proxy != null) {
                proxyProvider.returnProxy(proxy, page, task);
            }
        }
    }

    @Override
    public void setThread(int thread) {
        httpClientGenerator.setPoolSize(thread);
    }

    protected Page handleResponse(Request request, String charset, HttpResponse httpResponse, Task task) throws IOException {
        String content = getResponseContent(charset, httpResponse);
        Page page = new Page();
        page.setRawText(content);
        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        page.setDownloadSuccess(true);
        if (responseHeader) {
            page.setHeaders(HttpClientUtils.convertHeaders(httpResponse.getAllHeaders()));
        }
        return page;
    }

    protected Page handleFileResponse(Request request, String charset, HttpResponse httpResponse, Task task) throws IOException {
        downloadFile(httpResponse, request, task);
        Page page = new Page();
        page.setRequest(request);
        page.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        page.setDownloadSuccess(true);
        if (responseHeader) {
            page.setHeaders(HttpClientUtils.convertHeaders(httpResponse.getAllHeaders()));
        }
        return page;
    }

    private void downloadFile(HttpResponse httpResponse, Request request, Task task) throws IOException {
        String path = request.getRequestFileSavePath();
        if (StringUtils.isEmpty(path)) {
            path = BASE_FILE_SAVE_PATH
                    + task.getUUID() + File.separator
                    + request.getUrl().substring(request.getUrl().lastIndexOf("/")+1);
        }
        HttpEntity entity = httpResponse.getEntity();
        File file = new File(path);
        if (file.exists() && fileForceSave) {
            FileUtils.forceDelete(file);
        }
        file.getParentFile().mkdirs();
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        if (entity != null) {
            entity.writeTo(fos);
        }
        fos.flush();
        fos.close();
    }

    private String getResponseContent(String charset, HttpResponse httpResponse) throws IOException {
        if (charset == null) {
            byte[] contentBytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
            String htmlCharset = getHtmlCharset(httpResponse, contentBytes);
            if (htmlCharset != null) {
                return new String(contentBytes, htmlCharset);
            } else {
                logger.warn("Charset autodetect failed, use {} as charset. Please specify charset in Site.setCharset()", Charset.defaultCharset());
                return new String(contentBytes);
            }
        } else {
            return IOUtils.toString(httpResponse.getEntity().getContent(), charset);
        }
    }

    private String getHtmlCharset(HttpResponse httpResponse, byte[] contentBytes) throws IOException {
        return CharsetUtils.detectCharset(httpResponse.getEntity().getContentType() == null ? "" : httpResponse.getEntity().getContentType().getValue(), contentBytes);
    }
}

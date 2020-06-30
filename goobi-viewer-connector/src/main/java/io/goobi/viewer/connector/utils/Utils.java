/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.connector.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.connector.Version;
import io.goobi.viewer.connector.exceptions.HTTPException;
import io.goobi.viewer.connector.oai.RequestHandler;

/**
 * <p>
 * Utils class.
 * </p>
 *
 */
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private static final int HTTP_TIMEOUT = 10000;
    /** Constant <code>DEFAULT_ENCODING="UTF-8"</code> */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /** Constant <code>formatterISO8601DateTimeFullWithTimeZone</code> */
    public static DateTimeFormatter formatterISO8601DateTimeFullWithTimeZone = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * insert some chars in the time string
     *
     * @param milliSecondsAdd milliseconds to add to the current utc time if milliSecondsAdd = 0, no milli are added
     * @return the time in the format YYYY-MM-DDThh:mm:ssZ
     */
    public static String getCurrentUTCTime(long milliSecondsAdd) {
        return formatterISO8601DateTimeFullWithTimeZone.withZoneUTC().print(System.currentTimeMillis() + milliSecondsAdd);
    }

    /**
     * <p>
     * convertDate.
     * </p>
     *
     * @param milliSeconds a long.
     * @should convert time correctly
     * @return a {@link java.lang.String} object.
     */
    public static String convertDate(long milliSeconds) {
        return formatterISO8601DateTimeFullWithTimeZone.withZoneUTC().print(milliSeconds);
    }

    /**
     * <p>
     * getWebContentGET.
     * </p>
     *
     * @param urlString a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if any.
     */
    public static String getWebContentGET(String urlString) throws ClientProtocolException, IOException, HTTPException {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT)
                .setConnectTimeout(HTTP_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_TIMEOUT)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build()) {
            HttpGet get = new HttpGet(urlString);
            try (CloseableHttpResponse response = httpClient.execute(get); StringWriter writer = new StringWriter()) {
                int code = response.getStatusLine().getStatusCode();
                if (code == HttpStatus.SC_OK) {
                    return EntityUtils.toString(response.getEntity(), DEFAULT_ENCODING);
                }
                logger.trace("{}: {}; URL: {}", code, response.getStatusLine().getReasonPhrase(), urlString);
                throw new HTTPException(code, response.getStatusLine().getReasonPhrase());
            }
        }
    }

    /**
     * <p>
     * getWebContentPOST.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @param params a {@link java.util.Map} object.
     * @param cookies a {@link java.util.Map} object.
     * @return a {@link java.lang.String} object.
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if any.
     */
    public static String getWebContentPOST(String url, Map<String, String> params, Map<String, String> cookies)
            throws ClientProtocolException, IOException, HTTPException {
        if (url == null) {
            throw new IllegalArgumentException("url may not be null");
        }

        logger.trace("url: {}", url);
        List<NameValuePair> nameValuePairs = null;
        if (params == null) {
            nameValuePairs = new ArrayList<>(0);
        } else {
            nameValuePairs = new ArrayList<>(params.size());
            for (String key : params.keySet()) {
                // logger.trace("param: {}:{}", key, params.get(key)); // TODO do not log passwords!
                nameValuePairs.add(new BasicNameValuePair(key, params.get(key)));
            }
        }
        HttpClientContext context = null;
        CookieStore cookieStore = new BasicCookieStore();
        if (cookies != null && !cookies.isEmpty()) {
            context = HttpClientContext.create();
            for (String key : cookies.keySet()) {
                // logger.trace("cookie: {}:{}", key, cookies.get(key)); // TODO do not log passwords!
                BasicClientCookie cookie = new BasicClientCookie(key, cookies.get(key));
                cookie.setPath("/");
                cookie.setDomain("0.0.0.0");
                cookieStore.addCookie(cookie);
            }
            context.setCookieStore(cookieStore);
        }

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT)
                .setConnectTimeout(HTTP_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_TIMEOUT)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build()) {
            HttpPost post = new HttpPost(url);
            Charset.forName(DEFAULT_ENCODING);
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            try (CloseableHttpResponse response = (context == null ? httpClient.execute(post) : httpClient.execute(post, context));
                    StringWriter writer = new StringWriter()) {
                int code = response.getStatusLine().getStatusCode();
                if (code == HttpStatus.SC_OK) {
                    logger.trace("{}: {}", code, response.getStatusLine().getReasonPhrase());
                    IOUtils.copy(response.getEntity().getContent(), writer, DEFAULT_ENCODING);
                    return writer.toString();
                }
                logger.trace("{}: {}\n{}", code, response.getStatusLine().getReasonPhrase(),
                        IOUtils.toString(response.getEntity().getContent(), DEFAULT_ENCODING));
                throw new HTTPException(code, response.getStatusLine().getReasonPhrase());
            }
        }
    }

    /**
     * <p>
     * getHttpResponseStatus.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @return a int.
     * @throws java.lang.UnsupportedOperationException if any.
     * @throws java.io.IOException if any.
     */
    public static int getHttpResponseStatus(String url) throws UnsupportedOperationException, IOException {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT)
                .setConnectTimeout(HTTP_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_TIMEOUT)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build()) {
            HttpGet get = new HttpGet(url);
            Charset chars = Charset.forName(DEFAULT_ENCODING);
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                return response.getStatusLine().getStatusCode();
            }
        }
    }

    /**
     * Returns the application version number.
     *
     * @return a {@link java.lang.String} object.
     */
    public static String getVersion() {
        return Version.asJSON();
    }

    /**
     * <p>
     * splitIdentifierAndLanguageCode.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @param languageCodeLength a int.
     * @should split identifier correctly
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] splitIdentifierAndLanguageCode(String identifier, int languageCodeLength) {
        if (identifier == null) {
            throw new IllegalArgumentException("identifer may not be null");
        }
        if (languageCodeLength < 1) {
            throw new IllegalArgumentException("splitIndex must be 1 or larger");
        }

        String[] ret = new String[2];

        if (identifier.contains("_")) {
            int splitIndex = identifier.length() - languageCodeLength;
            ret[0] = identifier.substring(0, splitIndex - 1);
            ret[1] = identifier.substring(splitIndex);
        } else {
            ret[0] = identifier;
        }

        return ret;
    }

    /**
     * <p>
     * parseDate.
     * </p>
     *
     * @param datestring a {@link java.lang.Object} object.
     * @return a {@link java.lang.String} object.
     */
    public static String parseDate(Object datestring) {
        if (datestring instanceof Long) {
            return Utils.convertDate((Long) datestring);
        }
        return "";
    }

    /**
     * generates timestamp object from request
     *
     * @param requestHandler The request that was send to the server(servlet)
     * @return a HashMap with the values from, until and set as string
     * @should contain from timestamp
     * @should contain until timestamp
     * @should contain set
     * @should contain metadataPrefix
     * @should contain verb
     */
    public static Map<String, String> filterDatestampFromRequest(RequestHandler requestHandler) {
        Map<String, String> datestamp = new HashMap<>();

        String from = null;
        if (requestHandler.getFrom() != null) {
            from = requestHandler.getFrom();
            from = from.replace("-", "").replace("T", "").replace(":", "").replace("Z", "");
            datestamp.put("from", from);
        }
        String until = null;
        if (requestHandler.getUntil() != null) {
            until = requestHandler.getUntil();
            until = until.replace("-", "").replace("T", "").replace(":", "").replace("Z", "");
            datestamp.put("until", until);
        }

        String set = null;
        if (requestHandler.getSet() != null) {
            set = requestHandler.getSet();
            datestamp.put("set", set);
        }

        if (requestHandler.getMetadataPrefix() != null) {
            datestamp.put("metadataPrefix", requestHandler.getMetadataPrefix().getMetadataPrefix());
        }

        if (requestHandler.getVerb() != null) {
            datestamp.put("verb", requestHandler.getVerb().getTitle());
        }

        return datestamp;
    }
}

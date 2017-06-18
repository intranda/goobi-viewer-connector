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
package de.intranda.digiverso.m2m.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.m2m.Version;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private static final int HTTP_TIMEOUT = 10000;
    public static final String DEFAULT_ENCODING = "UTF-8";

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
     * 
     * @param milliSeconds
     * @return
     * @should convert time correctly
     */
    public static String convertDate(long milliSeconds) {
        return formatterISO8601DateTimeFullWithTimeZone.withZoneUTC().print(milliSeconds);
    }

    /**
     * Create a JDOM document from an XML string.
     * 
     * @param string
     * @param encoding
     * @return
     * @throws IOException
     * @throws JDOMException
     * @should build document correctly
     */
    public static Document getDocumentFromString(String string, String encoding) throws JDOMException, IOException {
        if (encoding == null) {
            encoding = DEFAULT_ENCODING;
        }

        byte[] byteArray = null;
        try {
            byteArray = string.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
        }
        ByteArrayInputStream baos = new ByteArrayInputStream(byteArray);
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(baos);

        return document;
    }

    /**
     * 
     * @param url
     * @return
     * @throws IOException
     * @throws UnsupportedOperationException
     */
    public static String getWebContent(String url) throws UnsupportedOperationException, IOException {
        logger.trace("getWebContent: {}", url);
        RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(HTTP_TIMEOUT).setConnectTimeout(HTTP_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_TIMEOUT).build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build()) {
            HttpGet get = new HttpGet(url);
            Charset chars = Charset.forName(DEFAULT_ENCODING);
            try (CloseableHttpResponse response = httpClient.execute(get); StringWriter writer = new StringWriter()) {
                int code = response.getStatusLine().getStatusCode();
                if (code == HttpStatus.SC_OK) {
                    logger.trace("{}: {}", code, response.getStatusLine().getReasonPhrase());
                    IOUtils.copy(response.getEntity().getContent(), writer);
                    return writer.toString();
                }
                logger.trace("{}: {}", code, response.getStatusLine().getReasonPhrase());
            }
        }

        return "";
    }

    public static int getHttpResponseStatus(String url) throws UnsupportedOperationException, IOException {
        //                logger.trace("getHttpReponseStatus: {}", url);
        RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(HTTP_TIMEOUT).setConnectTimeout(HTTP_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_TIMEOUT).build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build()) {
            HttpGet get = new HttpGet(url);
            Charset chars = Charset.forName(DEFAULT_ENCODING);
            try (CloseableHttpResponse response = httpClient.execute(get); StringWriter writer = new StringWriter()) {
                return response.getStatusLine().getStatusCode();
            }
        }
    }

    /**
     * Returns the application version number.
     * 
     * @return
     */
    public static String getVersion() {
        return Version.VERSION + "-" + Version.BUILDDATE + "-" + Version.BUILDVERSION;
    }

}

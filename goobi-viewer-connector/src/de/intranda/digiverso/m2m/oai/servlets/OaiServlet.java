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
package de.intranda.digiverso.m2m.oai.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.ProcessingInstruction;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.m2m.DataManager;
import de.intranda.digiverso.m2m.oai.RequestHandler;
import de.intranda.digiverso.m2m.oai.XMLGeneration;
import de.intranda.digiverso.m2m.oai.enums.Metadata;
import de.intranda.digiverso.m2m.oai.enums.Verb;
import de.intranda.digiverso.m2m.oai.model.ErrorCode;
import de.intranda.digiverso.m2m.utils.Utils;

public class OaiServlet extends HttpServlet {

    private static final long serialVersionUID = -2357047964682340928L;

    private final static Logger logger = LoggerFactory.getLogger(OaiServlet.class);

    private static DateTimeFormatter formatterISO8601Date = ISODateTimeFormat.date(); // yyyy-MM-dd
    private static DateTimeFormatter formatterISO8601DateTimeFullWithTimeZone = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        String queryString = (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        logger.debug("REQUEST URL: {}{}", request.getRequestURL().toString(), queryString);

        // HTTP request header dump
        //        Enumeration<String> headers = request.getHeaderNames();
        //        while (headers.hasMoreElements()) {
        //            String paramName = headers.nextElement();
        //            logger.debug("HTTP header: {} - {}", paramName, request.getHeader(paramName));
        //        }

        XMLGeneration xmlGeneration = new XMLGeneration();
        Document doc = new Document();
        //        ProcessingInstruction pi =
        //                new ProcessingInstruction("xml-stylesheet", "type='text/xsl' href='"
        //                        + request.getRequestURL().toString().replace(request.getServletPath(), "") + "/oai2.xsl'");
        ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type='text/xsl' href='./oai2.xsl'");

        doc.addContent(pi);
        // generate root element
        Element root = xmlGeneration.getOaiPmhElement("OAI-PMH");
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();

        Element responseDate = new Element("responseDate", xmlns);

        responseDate.setText(Utils.getCurrentUTCTime(0));
        root.addContent(responseDate);

        RequestHandler handler = new RequestHandler(request);
        // Element requestType = new Element("request", xmlns);
        // requestType.setAttribute("verb", handler.getVerb().getTitle());
        // requestType.setText(request.getRequestURL().toString());
        // root.addContent(requestType);

        // handle request
        if (handler.getVerb() == null) {
            Element requestType = new Element("request", xmlns);
            requestType.setAttribute("verb", "missing");
            if (DataManager.getInstance().getConfiguration().isBaseUrlUseInRequestElement()) {
                requestType.setText(DataManager.getInstance().getConfiguration().getBaseURL());
            } else {
                requestType.setText(request.getRequestURL().toString().replace("/M2M/", "/viewer/"));
            }
            root.addContent(requestType);
            root.addContent(new ErrorCode().getBadVerb());
        } else if (!checkDatestamps(handler.getFrom(), handler.getUntil())) {
            // Check for invalid from/until parameters
            root.addContent(new ErrorCode().getBadArgument());
        } else {
            // logger.debug("OAI request from " + request.getRemoteAddr() + ": " + handler.getVerb().getTitle() + " - " +
            // handler.getMetadataPrefix());

            Element requestType = new Element("request", xmlns);
            requestType.setAttribute("verb", handler.getVerb().getTitle());
            if (handler.getMetadataPrefix() != null) {
                requestType.setAttribute("metadataPrefix", handler.getMetadataPrefix().getMetadataPrefix());
            }
            if (StringUtils.isNotEmpty(handler.getIdentifier())) {
                requestType.setAttribute("identifier", handler.getIdentifier());
            }
            if (StringUtils.isNotEmpty(handler.getFrom())) {
                requestType.setAttribute("from", handler.getFrom());
            }
            if (StringUtils.isNotEmpty(handler.getUntil())) {
                requestType.setAttribute("until", handler.getUntil());
            }
            if (StringUtils.isNotEmpty(handler.getSet())) {
                requestType.setAttribute("set", handler.getSet());
            }
            if (DataManager.getInstance().getConfiguration().isBaseUrlUseInRequestElement()) {
                requestType.setText(DataManager.getInstance().getConfiguration().getBaseURL());
            } else {
                requestType.setText(request.getRequestURL().toString().replace("/M2M/", "/viewer/"));
            }
            root.addContent(requestType);
            // handle resumptionToken
            if (request.getParameter("resumptionToken") != null) {
                String resumptionToken = request.getParameterValues("resumptionToken")[0];
                requestType.setAttribute("resumptionToken", resumptionToken);
                root.addContent(xmlGeneration.handleToken(resumptionToken));
                xmlGeneration.removeExpiredTokens();
            }
            // handle Identify verb
            else if (handler.getVerb().equals(Verb.Identify)) {
                try {
                    root.addContent(xmlGeneration.getIdentifyXML());
                } catch (SolrServerException e) {
                    logger.error(e.getMessage(), e);
                    res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }
            }
            // handle ListIdentifiers verb
            else if (handler.getVerb().equals(Verb.ListIdentifiers)) {
                /*
                 * output for all metadata types is the same
                 */
                Element listIdentifiers;
                try {
                    int hitsPerToken = DataManager.getInstance().getConfiguration().getHitsPerToken();
                    listIdentifiers = xmlGeneration.createListIdentifiers(handler, 0, hitsPerToken);
                    root.addContent(listIdentifiers);
                } catch (SolrServerException e) {
                    logger.error(e.getMessage(), e);
                    res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }

            }
            // handle ListRecords verb
            else if (handler.getVerb().equals(Verb.ListRecords)) {
                if (handler.getMetadataPrefix() == null) {
                    root.addContent(new ErrorCode().getBadArgument());
                } else if (!DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(handler.getMetadataPrefix().name())) {
                    // Deny access to disabled formats
                    root.addContent(new ErrorCode().getCannotDisseminateFormat());
                } else {
                    if (handler.getUntil() == null) {
                        String until = Utils.convertDate(System.currentTimeMillis());
                        handler.setUntil(until);
                        logger.debug("No 'until' parameter, setting 'now' ({})", until);
                    }
                    try {
                        int hitsPerToken = DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(handler.getMetadataPrefix()
                                .name());
                        switch (handler.getMetadataPrefix()) {
                            case oai_dc:
                                Element listRecords = xmlGeneration.createListRecordsDC(handler, 0, hitsPerToken);
                                root.addContent(listRecords);
                                break;
                            case ese:
                                root.addContent(xmlGeneration.createListRecordsESE(handler, 0, hitsPerToken));
                                break;
                            case mets:
                                root.addContent(xmlGeneration.createListRecordsMets(handler, 0, hitsPerToken));
                                break;
                            case marcxml:
                                root.addContent(xmlGeneration.createListRecordsMarc(handler, 0, hitsPerToken));
                                break;
                            case epicur:
                                root.addContent(xmlGeneration.createListRecordsEpicur(handler, 0, hitsPerToken, "ListRecords"));
                                break;
                            case lido:
                                root.addContent(xmlGeneration.createListRecordsLido(handler, 0, hitsPerToken));
                                break;
                            case iv_overviewpage:
                            case iv_crowdsourcing:
                                root.addContent(xmlGeneration.createListRecordsIntrandaViewerUpdates(handler, 0, hitsPerToken, handler
                                        .getMetadataPrefix()));
                                break;
                            case tei:
                                root.addContent(xmlGeneration.createListRecordsTei(handler, 0, hitsPerToken));
                                break;
                            case cmdi:
                                root.addContent(xmlGeneration.createListRecordsCmdi(handler, 0, hitsPerToken));
                                break;
                            default:
                                root.addContent(new ErrorCode().getBadArgument());
                                break;
                        }
                    } catch (SolrServerException e) {
                        logger.error(e.getMessage(), e);
                        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    }
                }
            }
            // handle GetRecord verb
            else if (handler.getVerb().equals(Verb.GetRecord)) {
                if (handler.getMetadataPrefix() == null) {
                    root.addContent(new ErrorCode().getBadArgument());
                } else if (!DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(handler.getMetadataPrefix().name())) {
                    // Deny access to disabled formats
                    root.addContent(new ErrorCode().getCannotDisseminateFormat());
                } else {
                    switch (handler.getMetadataPrefix()) {
                        case mets:
                            root.addContent(xmlGeneration.createGetRecordMets(handler));
                            break;
                        case marcxml:
                            root.addContent(xmlGeneration.createGetRecordMarc(handler));
                            break;
                        case oai_dc:
                            root.addContent(xmlGeneration.createGetRecordDC(handler));
                            break;
                        case ese:
                            root.addContent(xmlGeneration.createGetRecordESE(handler));
                            break;
                        case epicur:
                            root.addContent(xmlGeneration.getRecordEpicur(handler));
                            break;
                        case lido:
                            root.addContent(xmlGeneration.createGetRecordLido(handler));
                            break;
                        case iv_overviewpage:
                        case iv_crowdsourcing:
                            root.addContent(xmlGeneration.createGetRecordIntrandaViewerUpdate(handler, handler.getMetadataPrefix()));
                            break;
                        case tei:
                            root.addContent(xmlGeneration.createGetRecordTei(handler));
                            break;
                        case cmdi:
                            root.addContent(xmlGeneration.createGetRecordCmdi(handler));
                            break;
                        default:
                            root.addContent(new ErrorCode().getCannotDisseminateFormat());
                            break;
                    }
                }
            } else if (handler.getVerb().equals(Verb.ListMetadataFormats)) {
                root.addContent(xmlGeneration.createMetadataFormats());
            } else if (handler.getVerb().equals(Verb.ListSets)) {
                try {
                    root.addContent(xmlGeneration.createListSets(DataManager.getInstance().getConfiguration().getDefaultLocale())); // TODO
                } catch (SolrServerException e) {
                    logger.error(e.getMessage(), e);
                    res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }
            } else {
                root.addContent(new ErrorCode().getBadArgument());
            }
        }
        doc.setRootElement(root);
        Format format = Format.getPrettyFormat();
        format.setEncoding("utf-8");
        XMLOutputter xmlOut = new XMLOutputter(format);
        if (handler.getMetadataPrefix() != null && handler.getMetadataPrefix().equals(Metadata.epicur)) {
            String ueblerhack = xmlOut.outputString(doc);
            ueblerhack = ueblerhack.replace("<epicur", "<epicur xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            res.setCharacterEncoding("utf-8");
            ServletOutputStream out = res.getOutputStream();
            out.print(ueblerhack);
        } else {
            xmlOut.output(doc, res.getOutputStream());
        }

    }

    /**
     * 
     * @param from
     * @param until
     * @return
     * @should return false if from is not well formed
     * @should return false if until is not well formed
     * @should return false if from after until
     * @should return true if from and until correct
     */
    public static boolean checkDatestamps(String from, String until) {
        boolean dateTime = false;
        if (from != null) {
            if (from.contains("T")) {
                // Date/time
                dateTime = true;
                try {
                    formatterISO8601DateTimeFullWithTimeZone.parseDateTime(from);
                } catch (IllegalArgumentException e) {
                    logger.debug(e.getMessage());
                    return false;
                }
            } else {
                // Just date
                try {
                    formatterISO8601Date.parseDateTime(from);
                } catch (IllegalArgumentException e) {
                    logger.debug(e.getMessage());
                    return false;
                }
            }
        }
        if (until != null) {
            if (until.contains("T")) {
                // Date/time
                dateTime = true;
                try {
                    formatterISO8601DateTimeFullWithTimeZone.parseDateTime(until);
                } catch (IllegalArgumentException e) {
                    return false;
                }
            } else {
                // Just date
                try {
                    formatterISO8601Date.parseDateTime(until);
                } catch (IllegalArgumentException e) {
                    logger.debug(e.getMessage());
                    return false;
                }
            }
        }
        if (from != null && until != null) {
            // Check for different from/until formats
            if (dateTime) {
                // 'from' may not be later than 'until'
                try {
                    if (formatterISO8601DateTimeFullWithTimeZone.parseDateTime(from).getMillis() > formatterISO8601DateTimeFullWithTimeZone
                            .parseDateTime(until).getMillis()) {
                        return false;
                    }
                } catch (IllegalArgumentException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            } else {
                // 'from' may not be later than 'until'
                try {
                    if (formatterISO8601Date.parseDateTime(from).getMillis() > formatterISO8601Date.parseDateTime(until).getMillis()) {
                        return false;
                    }
                } catch (IllegalArgumentException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }
}
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
package io.goobi.viewer.connector.oai.servlets;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.ProcessingInstruction;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.enums.Verb;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.oai.model.formats.Format;
import io.goobi.viewer.connector.utils.Utils;

/**
 * <p>
 * OaiServlet class.
 * </p>
 *
 */
public class OaiServlet extends HttpServlet {

    private static final long serialVersionUID = -2357047964682340928L;

    private static final Logger logger = LoggerFactory.getLogger(OaiServlet.class);

    /** {@inheritDoc} */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        String queryString = (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        logger.debug("REQUEST URL: {}{}", request.getRequestURL().toString(), queryString);

        Document doc = new Document();
        ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type='text/xsl' href='./oai2.xsl'");

        doc.addContent(pi);
        // generate root element
        Element root = Format.getOaiPmhElement("OAI-PMH");
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();

        Element responseDate = new Element("responseDate", xmlns);

        responseDate.setText(Utils.getCurrentUTCTime(LocalDateTime.now()));
        root.addContent(responseDate);

        RequestHandler handler = new RequestHandler(request);

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
            //  resumptionToken
            if (request.getParameter("resumptionToken") != null) {
                String resumptionToken = request.getParameterValues("resumptionToken")[0];
                requestType.setAttribute("resumptionToken", resumptionToken);
                root.addContent(Format.handleToken(resumptionToken));
                Format.removeExpiredTokens();
            }
            // Identify
            else if (handler.getVerb().equals(Verb.Identify)) {
                try {
                    root.addContent(Format.getIdentifyXML());
                } catch (SolrServerException e) {
                    logger.error(e.getMessage(), e);
                    res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }
            }
            // ListIdentifiers
            else if (handler.getVerb().equals(Verb.ListIdentifiers)) {
                if (handler.getMetadataPrefix() == null) {
                    root.addContent(new ErrorCode().getBadArgument());
                } else if (!DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(handler.getMetadataPrefix().name())) {
                    // Deny access to disabled formats
                    root.addContent(new ErrorCode().getCannotDisseminateFormat());
                } else {
                    try {
                        int hitsPerToken =
                                DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(handler.getMetadataPrefix().name());
                        String versionDiscriminatorField = DataManager.getInstance()
                                .getConfiguration()
                                .getVersionDisriminatorFieldForMetadataFormat(handler.getMetadataPrefix().name());
                        Format format = Format.getFormatByMetadataPrefix(handler.getMetadataPrefix());
                        if (format != null) {
                            root.addContent(format.createListIdentifiers(handler, 0, 0, hitsPerToken, versionDiscriminatorField));
                        } else {
                            root.addContent(new ErrorCode().getBadArgument());
                        }
                    } catch (SolrServerException e) {
                        logger.error(e.getMessage(), e);
                        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    }
                }
            }
            // ListRecords
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
                        int hitsPerToken =
                                DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(handler.getMetadataPrefix().name());
                        String versionDiscriminatorField = DataManager.getInstance()
                                .getConfiguration()
                                .getVersionDisriminatorFieldForMetadataFormat(handler.getMetadataPrefix().name());
                        logger.trace(handler.getMetadataPrefix().getMetadataPrefix());
                        Format format = Format.getFormatByMetadataPrefix(handler.getMetadataPrefix());
                        if (format != null) {
                            root.addContent(format.createListRecords(handler, 0, 0, hitsPerToken, versionDiscriminatorField));
                        } else {
                            root.addContent(new ErrorCode().getBadArgument());
                        }
                    } catch (SolrServerException e) {
                        logger.error(e.getMessage(), e);
                        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    }
                }
            }
            // GetRecord
            else if (handler.getVerb().equals(Verb.GetRecord)) {
                if (handler.getMetadataPrefix() == null) {
                    root.addContent(new ErrorCode().getBadArgument());
                } else if (!DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(handler.getMetadataPrefix().name())) {
                    // Deny access to disabled formats
                    root.addContent(new ErrorCode().getCannotDisseminateFormat());
                } else {
                    Format format = Format.getFormatByMetadataPrefix(handler.getMetadataPrefix());
                    if (format != null) {
                        root.addContent(format.createGetRecord(handler));
                    } else {
                        root.addContent(new ErrorCode().getBadArgument());
                    }
                }
            } else if (handler.getVerb().equals(Verb.ListMetadataFormats)) {
                root.addContent(Format.createMetadataFormats());
            } else if (handler.getVerb().equals(Verb.ListSets)) {
                try {
                    root.addContent(Format.createListSets(DataManager.getInstance().getConfiguration().getDefaultLocale())); // TODO
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
        org.jdom2.output.Format format = org.jdom2.output.Format.getPrettyFormat();
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
     * <p>
     * checkDatestamps.
     * </p>
     *
     * @param from a {@link java.lang.String} object.
     * @param until a {@link java.lang.String} object.
     * @should return false if from is not well formed
     * @should return false if until is not well formed
     * @should return false if from after until
     * @should return true if from and until correct
     * @return a boolean.
     */
    public static boolean checkDatestamps(String from, String until) {
        boolean dateTime = false;
        if (from != null) {
            if (from.contains("T")) {
                // Date/time
                dateTime = true;
                try {
                    LocalDateTime.parse(from, Utils.formatterISO8601DateTimeWithOffset);
                } catch (DateTimeParseException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            } else {
                // Just date
                try {
                    LocalDate.parse(from, Utils.formatterISO8601Date);
                } catch (DateTimeParseException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            }
        }
        if (until != null) {
            if (until.contains("T")) {
                // Date/time
                dateTime = true;
                try {
                    LocalDateTime.parse(until, Utils.formatterISO8601DateTimeWithOffset);
                } catch (DateTimeParseException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            } else {
                // Just date
                try {
                    LocalDate.parse(until, Utils.formatterISO8601Date);
                } catch (DateTimeParseException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            }
        }
        if (from != null && until != null) {
            // Check for different from/until formats ('from' may not be later than 'until')
            if (dateTime) {
                LocalDateTime ldtFrom = LocalDateTime.parse(from, Utils.formatterISO8601DateTimeWithOffset);
                LocalDateTime ldtUntil = LocalDateTime.parse(until, Utils.formatterISO8601DateTimeWithOffset);
                try {
                    return !ldtFrom.isAfter(ldtUntil);
                } catch (DateTimeParseException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            }

            LocalDate ldFrom = LocalDate.parse(from, Utils.formatterISO8601Date);
            LocalDate ldUntil = LocalDate.parse(until, Utils.formatterISO8601Date);
            try {
                return !ldFrom.isAfter(ldUntil);
            } catch (DateTimeParseException e) {
                logger.error(e.getMessage());
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }
}

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
package io.goobi.viewer.connector.oai.model.formats;

import java.io.File;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.oai.model.ResumptionToken;

public class FormatTest extends AbstractSolrEnabledTest {

    /**
     * @see Format#getIdentifyXML(String)
     * @verifies construct element correctly
     */
    @Test
    public void getIdentifyXML_shouldConstructElementCorrectly() throws Exception {
        Element eleIdentify = Format.getIdentifyXML(null);
        Assert.assertNotNull(eleIdentify);
        Assert.assertEquals("Identify", eleIdentify.getName());
        Assert.assertEquals("OAI Frontend", eleIdentify.getChildText("repositoryName", null));
        Assert.assertEquals("http://localhost:8080/viewer/oai", eleIdentify.getChildText("baseURL", null));
        Assert.assertEquals("2.0", eleIdentify.getChildText("protocolVersion", null));
        Assert.assertEquals("admin@example.com", eleIdentify.getChildText("adminEmail", null));
        Assert.assertEquals("2012-10-10T12:07:32Z", eleIdentify.getChildText("earliestDatestamp", null));
        Assert.assertEquals("transient", eleIdentify.getChildText("deletedRecord", null));
        Assert.assertEquals("YYYY-MM-DDThh:mm:ssZ", eleIdentify.getChildText("granularity", null));
    }

    /**
     * @see Format#createMetadataFormats()
     * @verifies construct element correctly
     */
    @Test
    public void createMetadataFormats_shouldConstructElementCorrectly() throws Exception {
        Element ele = Format.createMetadataFormats();
        Assert.assertNotNull(ele);
        Assert.assertEquals("ListMetadataFormats", ele.getName());
        List<Element> eleListMetadataFormat = ele.getChildren("metadataFormat", null);
        Assert.assertNotNull(eleListMetadataFormat);
        Assert.assertEquals(9, eleListMetadataFormat.size());
        for (Element eleMetadataFormat : eleListMetadataFormat) {
            Assert.assertNotNull(eleMetadataFormat.getChildText("metadataPrefix", null));
            Assert.assertNotNull(eleMetadataFormat.getChildText("metadataNamespace", null));
            Assert.assertNotNull(eleMetadataFormat.getChildText("schema", null));
        }
    }

    /**
     * @see Format#getOaiPmhElement(String)
     * @verifies construct element correctly
     */
    @Test
    public void getOaiPmhElement_shouldConstructElementCorrectly() throws Exception {
        Element ele = Format.getOaiPmhElement("oai");
        Assert.assertNotNull(ele);
        Assert.assertEquals("oai", ele.getName());
        Assert.assertEquals("http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd",
                ele.getAttributeValue("schemaLocation", Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")));
    }

    /**
     * @see Format#handleToken(String)
     * @verifies return error if resumption token name illegal
     */
    @Test
    public void handleToken_shouldReturnErrorIfResumptionTokenNameIllegal() throws Exception {
        Element result = Format.handleToken("foo", "");
        Assert.assertEquals("error", result.getName());
        Assert.assertEquals("badResumptionToken", result.getAttributeValue("code"));
    }

    /**
     * @see Format#deserializeResumptionToken(File)
     * @verifies deserialize token correctly
     */
    @Test
    public void deserializeResumptionToken_shouldDeserializeTokenCorrectly() throws Exception {
        File f = new File("src/test/resources/token/oai_1634822246437");
        Assert.assertTrue(f.isFile());

        ResumptionToken token = Format.deserializeResumptionToken(f);
        Assert.assertNotNull(token);
    }
}
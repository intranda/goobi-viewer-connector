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

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractTest;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.utils.SolrConstants;
import io.goobi.viewer.controller.XmlTools;

public class METSFormatTest extends AbstractTest {

    /**
     * @see METSFormat#generateMetsRecord(String,SolrDocument,RequestHandler,List,String)
     * @verifies generate element correctly
     */
    @Test
    public void generateMetsRecord_shouldGenerateElementCorrectly() throws Exception {
        Document metsDoc = XmlTools.readXmlFile("src/test/resources/viewer/indexed_mets/PPN517154005.xml");
        Assert.assertNotNull(metsDoc);

        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI_TOPSTRUCT, "PPN517154005");

        Element eleRecord =
                METSFormat.generateMetsRecord(XmlTools.getStringFromElement(metsDoc, StandardCharsets.UTF_8.name()), doc, new RequestHandler(),
                        null, null);
        Assert.assertNotNull(eleRecord);
        Assert.assertEquals("record", eleRecord.getName());
        Assert.assertNotNull(eleRecord.getChild("header", Format.OAI_NS));
        Element eleMetadata = eleRecord.getChild("metadata", Format.OAI_NS);
        Assert.assertNotNull(eleMetadata);
        Element eleMets = eleMetadata.getChild("mets", METSFormat.METS_NS);
        Assert.assertNotNull(eleMets);
    }
}
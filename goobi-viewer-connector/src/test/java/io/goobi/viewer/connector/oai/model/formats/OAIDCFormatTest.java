package io.goobi.viewer.connector.oai.model.formats;

import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.solr.SolrConstants;

class OAIDCFormatTest extends AbstractSolrEnabledTest {

    /**
     * @see OAIDCFormat#generateSingleDCRecord(SolrDocument,RequestHandler,String,Namespace,Namespace,List,String)
     * @verifies generate element correctly
     */
    @Test
    void generateSingleDCRecord_shouldGenerateElementCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI, "PPN123456789");
        doc.setField(SolrConstants.ACCESSCONDITION, "restricted");

        RequestHandler handler = new RequestHandler();

        Namespace nsOaiDoc = Namespace.getNamespace(Metadata.OAI_DC.getMetadataNamespacePrefix(), Metadata.OAI_DC.getMetadataNamespaceUri());

        OAIDCFormat format = new OAIDCFormat();
        Element eleRecord = format.generateSingleDCRecord(doc, handler, null, Format.OAI_NS, nsOaiDoc, null, null);
        Assertions.assertNotNull(eleRecord);

        Element eleHeader = eleRecord.getChild("header", Format.OAI_NS);
        Assertions.assertNotNull(eleHeader);
        Assertions.assertEquals("repoPPN123456789", eleHeader.getChildText("identifier", Format.OAI_NS));

        Element eleMetadata = eleRecord.getChild("metadata", Format.OAI_NS);
        Assertions.assertNotNull(eleMetadata);
        Element eleOaiDc = eleMetadata.getChild("dc", nsOaiDoc);
        Assertions.assertNotNull(eleOaiDc);
    }

    /**
     * @see OAIDCFormat#generateDcSource(SolrDocument,SolrDocument,SolrDocument,Namespace)
     * @verifies throw IllegalArgumentException if topstructDoc null
     */
    @Test
    void generateDcSource_shouldThrowIllegalArgumentExceptionIfTopstructDocNull() throws Exception {
        Namespace ns = Namespace.getNamespace(Metadata.DC.getMetadataNamespacePrefix(), Metadata.DC.getMetadataNamespaceUri());
        Assertions.assertThrows(IllegalArgumentException.class, () -> OAIDCFormat.generateDcSource(null, null, null, ns));
    }

    /**
     * @see OAIDCFormat#generateDcSource(SolrDocument,SolrDocument,SolrDocument,Namespace)
     * @verifies create element correctly
     */
    @Test
    void generateDcSource_shouldCreateElementCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField("MD_CREATOR", "Doe, John");
        doc.addField(SolrConstants.TITLE, "Foo Bar");
        doc.addField("MD_PLACEPUBLISH", "Somewhere");
        doc.addField("MD_PUBLISHER", "Indie");
        doc.addField("MD_YEARPUBLISH", "2023");

        Element ele = OAIDCFormat.generateDcSource(doc, doc, null,
                Namespace.getNamespace(Metadata.DC.getMetadataNamespacePrefix(), Metadata.DC.getMetadataNamespaceUri()));
        Assertions.assertNotNull(ele);
        Assertions.assertTrue(ele.getText().contains("Doe, John"));
        Assertions.assertTrue(ele.getText().contains("Foo Bar"));
        Assertions.assertTrue(ele.getText().contains("Somewhere"));
        Assertions.assertTrue(ele.getText().contains("Indie"));
        Assertions.assertTrue(ele.getText().contains("2023"));
    }
}
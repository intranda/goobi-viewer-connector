package io.goobi.viewer.connector.oai.model.formats;

import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.solr.SolrConstants;

public class OAIDCFormatTest extends AbstractSolrEnabledTest {

    /**
     * @see OAIDCFormat#generateSingleDCRecord(SolrDocument,RequestHandler,String,Namespace,Namespace,List,String)
     * @verifies generate element correctly
     */
    @Test
    public void generateSingleDCRecord_shouldGenerateElementCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.PI, "PPN123456789");

        RequestHandler handler = new RequestHandler();

        Namespace nsOaiDoc = Namespace.getNamespace(Metadata.OAI_DC.getMetadataNamespacePrefix(), Metadata.OAI_DC.getMetadataNamespaceUri());

        OAIDCFormat format = new OAIDCFormat();
        Element eleRecord = format.generateSingleDCRecord(doc, handler, null, Format.OAI_NS, nsOaiDoc, null, null);
        Assert.assertNotNull(eleRecord);

        Element eleHeader = eleRecord.getChild("header", Format.OAI_NS);
        Assert.assertNotNull(eleHeader);
        Assert.assertEquals("repoPPN123456789", eleHeader.getChildText("identifier", Format.OAI_NS));

        Element eleMetadata = eleRecord.getChild("metadata", Format.OAI_NS);
        Assert.assertNotNull(eleMetadata);
        Element eleOaiDc = eleMetadata.getChild("dc", nsOaiDoc);
        Assert.assertNotNull(eleOaiDc);
    }

    /**
     * @see OAIDCFormat#generateDcSource(SolrDocument,SolrDocument,SolrDocument,Namespace)
     * @verifies throw IllegalArgumentException if topstructDoc null
     */
    @Test(expected = IllegalArgumentException.class)
    public void generateDcSource_shouldThrowIllegalArgumentExceptionIfTopstructDocNull() throws Exception {
        OAIDCFormat.generateDcSource(null, null, null,
                Namespace.getNamespace(Metadata.DC.getMetadataNamespacePrefix(), Metadata.DC.getMetadataNamespaceUri()));
    }

    /**
     * @see OAIDCFormat#generateDcSource(SolrDocument,SolrDocument,SolrDocument,Namespace)
     * @verifies create element correctly
     */
    @Test
    public void generateDcSource_shouldCreateElementCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField("MD_CREATOR", "Doe, John");
        doc.addField(SolrConstants.TITLE, "Foo Bar");
        doc.addField("MD_PLACEPUBLISH", "Somewhere");
        doc.addField("MD_PUBLISHER", "Indie");
        doc.addField("MD_YEARPUBLISH", "2023");
        
        Element ele = OAIDCFormat.generateDcSource(doc, doc, null,
                Namespace.getNamespace(Metadata.DC.getMetadataNamespacePrefix(), Metadata.DC.getMetadataNamespaceUri()));
        Assert.assertNotNull(ele);
        Assert.assertTrue(ele.getText().contains("Doe, John"));
        Assert.assertTrue(ele.getText().contains("Foo Bar"));
        Assert.assertTrue(ele.getText().contains("Somewhere"));
        Assert.assertTrue(ele.getText().contains("Indie"));
        Assert.assertTrue(ele.getText().contains("2023"));
    }

}
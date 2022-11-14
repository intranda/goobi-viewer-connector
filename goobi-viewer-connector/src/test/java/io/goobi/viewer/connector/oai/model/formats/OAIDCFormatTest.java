package io.goobi.viewer.connector.oai.model.formats;

import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.utils.SolrConstants;

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

        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Namespace nsOaiDoc = Namespace.getNamespace(Metadata.OAI_DC.getMetadataNamespacePrefix(), Metadata.OAI_DC.getMetadataNamespaceUri());

        OAIDCFormat format = new OAIDCFormat();
        Element eleRecord = format.generateSingleDCRecord(doc, handler, null, xmlns, nsOaiDoc, null, null);
        Assert.assertNotNull(eleRecord);
        
        Element eleHeader = eleRecord.getChild("header", xmlns);
        Assert.assertNotNull(eleHeader);
        Assert.assertEquals("repoPPN123456789", eleHeader.getChildText("identifier", xmlns));
       
        Element eleMetadata = eleRecord.getChild("metadata", xmlns);
        Assert.assertNotNull(eleMetadata);
        Element eleOaiDc = eleMetadata.getChild("dc", nsOaiDoc);
        Assert.assertNotNull(eleOaiDc);
    }
}
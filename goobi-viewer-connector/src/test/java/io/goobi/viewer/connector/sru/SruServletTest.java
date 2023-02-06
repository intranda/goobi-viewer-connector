package io.goobi.viewer.connector.sru;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.oai.enums.Metadata;

public class SruServletTest extends AbstractSolrEnabledTest {

    /**
     * @see SruServlet#createSchemaInfoElement(String,String,String,String)
     * @verifies create element correctly
     */
    @Test
    public void createSchemaInfoElement_shouldCreateElementCorrectly() throws Exception {
        Element ele = SruServlet.createSchemaInfoElement("Schema F", "sf", "info:srw/schema/1/f-1.0", "http://example.com/schemaf.xsd");
        Assert.assertNotNull(ele);
        Element eleSchema = ele.getChild("schema", SruServlet.EXPLAIN_NAMESPACE);
        Assert.assertNotNull(eleSchema);
        Assert.assertEquals("true", eleSchema.getAttributeValue("retrieve"));
        Assert.assertEquals("false", eleSchema.getAttributeValue("sort"));
        Assert.assertEquals("info:srw/schema/1/f-1.0", eleSchema.getAttributeValue("identifier"));
        Assert.assertEquals("http://example.com/schemaf.xsd", eleSchema.getAttributeValue("location"));
        Assert.assertEquals("sf", eleSchema.getAttributeValue("name"));
        Assert.assertEquals("Schema F", eleSchema.getChildText("title", SruServlet.EXPLAIN_NAMESPACE));

    }

    /**
     * @see SruServlet#createSupportsElement(String)
     * @verifies create element correctly
     */
    @Test
    public void createSupportsElement_shouldCreateElementCorrectly() throws Exception {
        Element ele = SruServlet.createSupportsElement("your mom");
        Assert.assertNotNull(ele);
        Assert.assertEquals("your mom", ele.getText());
    }

    /**
     * @see SruServlet#createWrongSchemaDocument(SruRequestParameter,String)
     * @verifies create document correctly
     */
    @Test
    public void createWrongSchemaDocument_shouldCreateDocumentCorrectly() throws Exception {
        Document doc = SruServlet.createWrongSchemaDocument("1.2", "sf");
        Assert.assertNotNull(doc);
        Element eleRoot = doc.getRootElement();
        Assert.assertNotNull(eleRoot);
        Assert.assertEquals("1.2", eleRoot.getChildText("version", SruServlet.SRU_NAMESPACE));

        Element eleDiagnostic = eleRoot.getChild("diagnostic", SruServlet.SRU_NAMESPACE);
        Assert.assertNotNull(eleDiagnostic);
        Element eleUri = eleDiagnostic.getChild("uri", SruServlet.DIAG_NAMESPACE);
        Assert.assertNotNull(eleUri);
        Assert.assertEquals("info:srw/diagnostic/1/66", eleUri.getText());
        Assert.assertEquals("Unknown schema for retrieval / sf", eleDiagnostic.getChildText("message", SruServlet.DIAG_NAMESPACE));
    }

    /**
     * @see SruServlet#createMissingArgumentDocument(String,String)
     * @verifies create document correctly
     */
    @Test
    public void createMissingArgumentDocument_shouldCreateDocumentCorrectly() throws Exception {
        Document doc = SruServlet.createMissingArgumentDocument("1.1", "scanClause");
        Assert.assertNotNull(doc);
        Element eleRoot = doc.getRootElement();
        Assert.assertNotNull(eleRoot);
        Assert.assertEquals("1.1", eleRoot.getChildText("version", SruServlet.SRU_NAMESPACE));

        Element eleDiagnostic = eleRoot.getChild("diagnostic", SruServlet.SRU_NAMESPACE);
        Assert.assertNotNull(eleDiagnostic);
        Element eleUri = eleDiagnostic.getChild("uri", SruServlet.DIAG_NAMESPACE);
        Assert.assertNotNull(eleUri);
        Assert.assertEquals("info:srw/diagnostic/1/7", eleUri.getText());
        Assert.assertEquals("Mandatory parameter not supplied / scanClause", eleDiagnostic.getChildText("message", SruServlet.DIAG_NAMESPACE));
    }

    /**
     * @see SruServlet#createUnsupportedOperationDocument(String,String)
     * @verifies create document correctly
     */
    @Test
    public void createUnsupportedOperationDocument_shouldCreateDocumentCorrectly() throws Exception {
        Document doc = SruServlet.createUnsupportedOperationDocument("1.2", "foobar");
        Assert.assertNotNull(doc);
        Element eleRoot = doc.getRootElement();
        Assert.assertNotNull(eleRoot);
        Assert.assertEquals("1.2", eleRoot.getChildText("version", SruServlet.SRU_NAMESPACE));

        Element eleDiagnostic = eleRoot.getChild("diagnostic", SruServlet.SRU_NAMESPACE);
        Assert.assertNotNull(eleDiagnostic);
        Element eleUri = eleDiagnostic.getChild("uri", SruServlet.DIAG_NAMESPACE);
        Assert.assertNotNull(eleUri);
        Assert.assertEquals("info:srw/diagnostic/1/4", eleUri.getText());
        Assert.assertEquals("Unsupported operation / foobar", eleDiagnostic.getChildText("message", SruServlet.DIAG_NAMESPACE));
    }

    /**
     * @see SruServlet#generateSearchQuery(String,Metadata,String)
     * @verifies create query correctly
     */
    @Test
    public void generateSearchQuery_shouldCreateQueryCorrectly() throws Exception {
        String result = SruServlet.generateSearchQuery("dc.identifier=urn:nbn:foo;bar:123", Metadata.LIDO, " -DC:a.*");
        Assert.assertEquals("URN:urn:nbn:foo;bar:123 AND SOURCEDOCFORMAT:LIDO AND (ISWORK:true OR ISANCHOR:true) -DC:a.*", result);
        
        result = SruServlet.generateSearchQuery("anywhere=foo", Metadata.MARCXML, null);
        Assert.assertEquals("*:foo AND SOURCEDOCFORMAT:METS AND (ISWORK:true OR ISANCHOR:true)", result);
    }
}
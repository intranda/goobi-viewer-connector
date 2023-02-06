package io.goobi.viewer.connector.sru;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

public class SruServletTest {

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
}
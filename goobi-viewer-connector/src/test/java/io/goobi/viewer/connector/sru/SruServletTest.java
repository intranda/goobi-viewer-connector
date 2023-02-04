package io.goobi.viewer.connector.sru;

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
}
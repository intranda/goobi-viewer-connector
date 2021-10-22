package io.goobi.viewer.connector.oai.model.formats;

import java.io.File;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractTest;
import io.goobi.viewer.connector.oai.model.ResumptionToken;

public class FormatTest extends AbstractTest {

    /**
     * @see Format#handleToken(String)
     * @verifies return error if resumption token name illegal
     */
    @Test
    public void handleToken_shouldReturnErrorIfResumptionTokenNameIllegal() throws Exception {
        Element result = Format.handleToken("foo");
        Assert.assertEquals("error", result.getName());
        Assert.assertEquals("badResumptionToken", result.getAttributeValue("code"));
    }

    /**
     * @see Format#deserializeResumptionToken(File)
     * @verifies deserialize token correctly
     */
    @Test
    public void deserializeResumptionToken_shouldDeserializeTokenCorrectly() throws Exception {
        File f= new File("src/test/resources/token/oai_1634822246437");
        Assert.assertTrue(f.isFile());
        
        ResumptionToken token = Format.deserializeResumptionToken(f);
        Assert.assertNotNull(token);
    }
}
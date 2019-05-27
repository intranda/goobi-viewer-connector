package de.intranda.digiverso.m2m.oai.model.formats;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

public class FormatTest {

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
}
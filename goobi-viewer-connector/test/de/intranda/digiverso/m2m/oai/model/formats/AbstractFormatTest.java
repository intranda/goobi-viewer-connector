package de.intranda.digiverso.m2m.oai.model.formats;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

public class AbstractFormatTest {

    /**
     * @see AbstractFormat#handleToken(String)
     * @verifies return error if resumption token name illegal
     */
    @Test
    public void handleToken_shouldReturnErrorIfResumptionTokenNameIllegal() throws Exception {
        Element result = AbstractFormat.handleToken("foo");
        Assert.assertEquals("error", result.getName());
        Assert.assertEquals("badResumptionToken", result.getAttributeValue("code"));
    }
}
package de.intranda.digiverso.m2m.oai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.m2m.utils.SolrConstants;

public class XMLGenerationTest {

    /**
     * @see XMLGeneration#getAdditionalDocstructsQuerySuffix()
     * @verifies build query suffix correctly
     */
    @Test
    public void getAdditionalDocstructsQuerySuffix_shouldBuildQuerySuffixCorrectly() throws Exception {
        Assert.assertEquals(" OR (" + SolrConstants.DOCTYPE + ":DOCSTRCT AND (" + SolrConstants.DOCSTRCT + ":Article))", XMLGeneration
                .getAdditionalDocstructsQuerySuffix(Collections.singletonList("Article")));
    }

    /**
     * @see XMLGeneration#getUrnPrefixBlacklistSuffix(List)
     * @verifies build query suffix correctly
     */
    @Test
    public void getUrnPrefixBlacklistSuffix_shouldBuildQuerySuffixCorrectly() throws Exception {
        List<String> prefixes = new ArrayList<>();
        prefixes.add("urn:nbn:de:test-1");
        prefixes.add("urn:nbn:de:hidden-1");
        Assert.assertEquals(
                " -URN_UNTOKENIZED:urn\\:nbn\\:de\\:test\\-1* -IMAGEURN_UNTOKENIZED:urn\\:nbn\\:de\\:test\\-1* -IMAGEURN_OAI_UNTOKENIZED:urn\\:nbn\\:de\\:test\\-1* -URN_UNTOKENIZED:urn\\:nbn\\:de\\:hidden\\-1* -IMAGEURN_UNTOKENIZED:urn\\:nbn\\:de\\:hidden\\-1* -IMAGEURN_OAI_UNTOKENIZED:urn\\:nbn\\:de\\:hidden\\-1*",
                XMLGeneration.getUrnPrefixBlacklistSuffix(prefixes));
    }
}
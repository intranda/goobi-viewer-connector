/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.connector.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Test;

public class SolrSearchToolsTest {

    /**
     * @see SolrSearchTools#getAdditionalDocstructsQuerySuffix(List)
     * @verifies build query suffix correctly
     */
    @Test
    public void getAdditionalDocstructsQuerySuffix_shouldBuildQuerySuffixCorrectly() throws Exception {
        Assert.assertEquals(" OR (" + SolrConstants.DOCTYPE + ":DOCSTRCT AND (" + SolrConstants.DOCSTRCT + ":Article))",
                SolrSearchTools.getAdditionalDocstructsQuerySuffix(Collections.singletonList("Article")));
    }

    /**
     * @see SolrSearchTools#getLatestValidDateUpdated(SolrDocument,long)
     * @verifies return 0 if no valid value is found
     */
    @Test
    public void getLatestValidDateUpdated_shouldReturn0IfNoValidValueIsFound() throws Exception {
        SolrDocument doc = new SolrDocument();
        Assert.assertEquals(Long.valueOf(0), SolrSearchTools.getLatestValidDateUpdated(doc, System.currentTimeMillis()));
    }

    /**
     * @see SolrSearchTools#getLatestValidDateUpdated(SolrDocument,long)
     * @verifies return correct value
     */
    @Test
    public void getLatestValidDateUpdated_shouldReturnCorrectValue() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.DATEUPDATED, 1L);
        doc.addField(SolrConstants.DATEUPDATED, 2L);
        doc.addField(SolrConstants.DATEUPDATED, 3L);
        doc.addField(SolrConstants.DATEUPDATED, 4L);
        Assert.assertEquals(Long.valueOf(3), SolrSearchTools.getLatestValidDateUpdated(doc, 3));
    }

    /**
     * @see SolrSearchTools#getLatestValidDateUpdated(SolrDocument,long)
     * @verifies ignore untilTimestamp if zero
     */
    @Test
    public void getLatestValidDateUpdated_shouldIgnoreUntilTimestampIfZero() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.DATEUPDATED, 1L);
        doc.addField(SolrConstants.DATEUPDATED, 2L);
        doc.addField(SolrConstants.DATEUPDATED, 3L);
        doc.addField(SolrConstants.DATEUPDATED, 4L);
        Assert.assertEquals(Long.valueOf(4), SolrSearchTools.getLatestValidDateUpdated(doc, 0));
    }

    /**
     * @see SolrSearchTools#getMetadataValues(SolrDocument,String)
     * @verifies return all values for the given field
     */
    @Test
    public void getMetadataValues_shouldReturnAllValuesForTheGivenField() throws Exception {
        SolrDocument doc = new SolrDocument();
        for (int i = 1; i <= 5; ++i) {
            doc.addField("MDNUM_FOO", i);
        }
        List<String> result = SolrSearchTools.getMetadataValues(doc, "MDNUM_FOO");
        Assert.assertEquals(5, result.size());
    }

    /**
     * @see SolrSearchTools#getUrnPrefixBlacklistSuffix(List)
     * @verifies build query suffix correctly
     */
    @Test
    public void getUrnPrefixBlacklistSuffix_shouldBuildQuerySuffixCorrectly() throws Exception {
        List<String> prefixes = new ArrayList<>();
        prefixes.add("urn:nbn:de:test-1");
        prefixes.add("urn:nbn:de:hidden-1");
        Assert.assertEquals(
                " -URN_UNTOKENIZED:urn\\:nbn\\:de\\:test\\-1* -IMAGEURN_UNTOKENIZED:urn\\:nbn\\:de\\:test\\-1* -IMAGEURN_OAI_UNTOKENIZED:urn\\:nbn\\:de\\:test\\-1* -URN_UNTOKENIZED:urn\\:nbn\\:de\\:hidden\\-1* -IMAGEURN_UNTOKENIZED:urn\\:nbn\\:de\\:hidden\\-1* -IMAGEURN_OAI_UNTOKENIZED:urn\\:nbn\\:de\\:hidden\\-1*",
                SolrSearchTools.getUrnPrefixBlacklistSuffix(prefixes));
    }

    /**
     * @see SolrSearchTools#buildQueryString(String,String,String,String,boolean,String)
     * @verifies add from until to setSpec queries
     */
    @Test
    public void buildQueryString_shouldAddFromUntilToSetSpecQueries() throws Exception {
        String query = SolrSearchTools.buildQueryString("2022-10-27T16:00:00Z", "2022-10-27T16:15:00Z", "goobi", "oai_dc", false, null);
        Assert.assertTrue(query.contains(" +DATEUPDATED:[1666886400000 TO 1666887300999]"));
    }
}
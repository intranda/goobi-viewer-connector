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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.solr.SolrConstants;

public class SolrSearchIndexTest extends AbstractSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SolrSearchIndexTest.class);

    /**
     * @see SolrSearchIndex#getSets(String)
     * @verifies return all values
     */
    @Test
    public void getSets_shouldReturnAllValues() throws Exception {
        Assert.assertEquals(43, DataManager.getInstance()
                .getSearchIndex()
                .getSets(SolrConstants.DC)
                .size());
    }

    /**
     * @see SolrSearchIndex#getFirstDoc(String,List)
     * @verifies return correct doc
     */
    @Test
    public void getFirstDoc_shouldReturnCorrectDoc() throws Exception {
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc(SolrConstants.PI + ":PPN517154005", Collections.singletonList(SolrConstants.PI));
        Assert.assertNotNull(doc);
        Assert.assertEquals("PPN517154005", doc.getFieldValue(SolrConstants.PI));
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,List,Map)
     * @verifies return correct number of rows
     */
    @Test
    public void search_shouldReturnCorrectNumberOfRows() throws Exception {
        QueryResponse qr = DataManager.getInstance()
                .getSearchIndex()
                .search(null, null, null, Metadata.OAI_DC.getMetadataPrefix(), 0, 55, false, null, "", null, null);
        Assert.assertEquals(55, qr.getResults()
                .size());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,List,Map)
     * @verifies sort results correctly
     */
    @Test
    public void search_shouldSortResultsCorrectly() throws Exception {
        QueryResponse qr = DataManager.getInstance()
                .getSearchIndex()
                .search(null, null, null, Metadata.OAI_DC.getMetadataPrefix(), 0, 10, false, null, "", null, null);
        Assert.assertFalse(qr.getResults()
                .isEmpty());
        long previous = 0;
        for (SolrDocument doc : qr.getResults()) {
            Long dateCreated = (long) doc.getFieldValue(SolrConstants.DATECREATED);
            Assert.assertNotNull(dateCreated);
            Assert.assertTrue(dateCreated > previous);
            previous = dateCreated;
        }
    }

    /**
     * @see SolrSearchIndex#getFulltextFileNames(String)
     * @verifies return file names correctly
     */
    @Test
    public void getFulltextFileNames_shouldReturnFileNamesCorrectly() throws Exception {
        Map<Integer, String> result = DataManager.getInstance()
                .getSearchIndex()
                .getFulltextFileNames("PPN517154005");
        Assert.assertEquals(14, result.size());
        Assert.assertEquals("alto/PPN517154005/00000001.xml", result.get(1));
    }
}
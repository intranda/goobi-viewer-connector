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
package de.intranda.digiverso.m2m.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.m2m.AbstractSolrEnabledTest;
import de.intranda.digiverso.m2m.DataManager;
import de.intranda.digiverso.m2m.oai.enums.Metadata;

public class SolrSearchIndexTest extends AbstractSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SolrSearchIndexTest.class);

    /**
     * @see SolrSearchIndex#getSets(String)
     * @verifies return all values
     */
    @Test
    public void getSets_shouldReturnAllValues() throws Exception {
        Assert.assertEquals(23, DataManager.getInstance()
                .getSearchIndex()
                .getSets(SolrConstants.DC)
                .size());
    }

    /**
     * @see SolrSearchIndex#getLatestValidDateUpdated(SolrDocument,long)
     * @verifies return correct value
     */
    @Test
    public void getLatestValidDateUpdated_shouldReturnCorrectValue() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.DATEUPDATED, 1L);
        doc.addField(SolrConstants.DATEUPDATED, 2L);
        doc.addField(SolrConstants.DATEUPDATED, 3L);
        doc.addField(SolrConstants.DATEUPDATED, 4L);
        Assert.assertEquals(Long.valueOf(3), SolrSearchIndex.getLatestValidDateUpdated(doc, 3));
    }

    /**
     * @see SolrSearchIndex#getLatestValidDateUpdated(SolrDocument,long)
     * @verifies return 0 if no valid value is found
     */
    @Test
    public void getLatestValidDateUpdated_shouldReturn0IfNoValidValueIsFound() throws Exception {
        SolrDocument doc = new SolrDocument();
        Assert.assertEquals(Long.valueOf(0), SolrSearchIndex.getLatestValidDateUpdated(doc, System.currentTimeMillis()));
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
                .search(null, null, null, Metadata.oai_dc.name(), 0, 55, false, null, null);
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
                .search(null, null, null, Metadata.oai_dc.name(), 0, 10, false, null, null);
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
}
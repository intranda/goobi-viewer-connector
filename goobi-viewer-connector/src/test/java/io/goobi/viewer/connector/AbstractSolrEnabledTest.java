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
package io.goobi.viewer.connector;

import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import io.goobi.viewer.connector.utils.SolrSearchIndex;

/**
 * JUnit test classes that extend this class can use the embedded Solr server setup with a fixed viewer index.
 */
public abstract class AbstractSolrEnabledTest extends AbstractTest {

    private static final String SOLR_TEST_URL = "https://viewer-testing-index.goobi.io/solr/collection1";

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractTest.setUpClass();
    }

    @Before
    public void setUp() throws Exception {
        HttpSolrClient client = SolrSearchIndex.getNewHttpSolrClient(SOLR_TEST_URL);
        DataManager.getInstance().injectSearchIndex(new SolrSearchIndex(client, true));
    }

    @After
    public void tearDown() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        AbstractTest.tearDownClass();
    }
}
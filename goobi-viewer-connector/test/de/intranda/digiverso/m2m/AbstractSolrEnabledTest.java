/**
 * This file is part of the Goobi Viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package de.intranda.digiverso.m2m;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import de.intranda.digiverso.m2m.utils.Configuration;
import de.intranda.digiverso.m2m.utils.SolrSearchIndex;

/**
 * JUnit test classes that extend this class can use the embedded Solr server setup with a fixed viewer index.
 */
public abstract class AbstractSolrEnabledTest {

    private static final String CORE_NAME = "test-viewer";

    // private static String solrPath = "/opt/digiverso/viewer/apache-solr/";
    private static String solrUrl = "http://localhost:8080/solr";
    // private static CoreContainer coreContainer;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_oai.test.xml"));

        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0) {
            // solrPath = "C:/digiverso/viewer/apache-solr-test/";
            solrUrl = "http://localhost:8081/solr";
        }
        //        File solrDir = new File(solrPath);
        //        Assert.assertTrue("Solr folder not found in " + solrPath, solrDir.isDirectory());
        //
        //        coreContainer = new CoreContainer(solrPath);
        //        coreContainer.load();
    }

    @Before
    public void setUp() throws Exception {
        //        Assert.assertTrue("Core '" + CORE_NAME + "' is not loaded.", coreContainer.isLoaded(CORE_NAME));
        //        EmbeddedSolrServer server = new EmbeddedSolrServer(coreContainer, CORE_NAME);
        HttpSolrServer server = SolrSearchIndex.getNewHttpSolrServer(solrUrl + '/' + CORE_NAME);
        DataManager.getInstance().injectSearchIndex(new SolrSearchIndex(server, true));
    }

    @After
    public void tearDown() throws Exception {
        //        if (coreContainer != null) {
        //            coreContainer.getClass(); // random call so that there is no red PMD warning
        //        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        //        if (coreContainer != null) {
        //            coreContainer.shutdown();
        //        }
    }
}
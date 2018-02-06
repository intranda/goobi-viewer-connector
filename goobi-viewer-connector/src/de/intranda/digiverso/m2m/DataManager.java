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
package de.intranda.digiverso.m2m;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.m2m.oai.model.language.LanguageHelper;
import de.intranda.digiverso.m2m.utils.Configuration;
import de.intranda.digiverso.m2m.utils.SolrSearchIndex;

public final class DataManager {

    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);

    private static final Object lock = new Object();

    private static volatile DataManager instance = null;

    private Configuration configuration;

    private SolrSearchIndex searchIndex;

    private LanguageHelper languageHelper;

    public static DataManager getInstance() {
        DataManager dm = instance;
        if (dm == null) {
            synchronized (lock) {
                // Another thread might have initialized instance by now
                dm = instance;
                if (dm == null) {
                    dm = new DataManager();
                    instance = dm;
                }
            }
        }

        return dm;
    }

    private DataManager() {
    }

    /**
     * @return the configuration
     */
    public Configuration getConfiguration() {
        if (configuration == null) {
            synchronized (lock) {
                configuration = new Configuration(Configuration.DEFAULT_CONFIG_FILE);
            }
        }

        return configuration;
    }

    /**
     * @return the searchIndex
     */
    public SolrSearchIndex getSearchIndex() {
        if (searchIndex == null) {
            synchronized (lock) {
                searchIndex = new SolrSearchIndex(null, false);
            }
        }
        searchIndex.checkReloadNeeded();

        return searchIndex;
    }

    /**
     * @return the languageHelper
     */
    public LanguageHelper getLanguageHelper() {
        if (languageHelper == null) {
            synchronized (lock) {
                String configFolder = getConfiguration().getViewerConfigFolder();
                if (!configFolder.endsWith("/")) {
                    configFolder += '/';
                }
                languageHelper = new LanguageHelper(configFolder + "languages.xml");
            }
        }

        return languageHelper;
    }

    /**
     * Sets custom Configuration object (used for unit testing).
     * 
     * @param dao
     */
    public void injectConfiguration(Configuration configuration) {
        if (configuration != null) {
            this.configuration = configuration;
        }
    }

    /**
     * Sets custom SolrSearchIndex object (used for unit testing).
     * 
     * @param dao
     */
    public void injectSearchIndex(SolrSearchIndex searchIndex) {
        if (searchIndex != null) {
            this.searchIndex = searchIndex;
        }
    }
}

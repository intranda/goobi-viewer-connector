/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
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
package io.goobi.viewer.connector.oai.model.language;

import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.reloading.PeriodicReloadingTrigger;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.connector.utils.Configuration;

/**
 * <p>
 * LanguageHelper class.
 * </p>
 *
 */
public class LanguageHelper {

    private static final Logger logger = LogManager.getLogger(LanguageHelper.class);

    ReloadingFileBasedConfigurationBuilder<XMLConfiguration> builder;

    /**
     * <p>
     * Constructor for LanguageHelper.
     * </p>
     *
     * @param configFilePath a {@link java.lang.String} object.
     */
    public LanguageHelper(String configFilePath) {
        try {
            builder =
                    new ReloadingFileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
                            .configure(new Parameters().properties()
                                    .setFileName(configFilePath)
                                    .setListDelimiterHandler(new DefaultListDelimiterHandler(';'))
                                    .setThrowExceptionOnMissing(false));
            builder.getConfiguration().setExpressionEngine(new XPathExpressionEngine());
            PeriodicReloadingTrigger trigger = new PeriodicReloadingTrigger(builder.getReloadingController(),
                    null, 10, TimeUnit.SECONDS);
            trigger.start();
        } catch (ConfigurationException e) {
            logger.error(e.getMessage());
        }
    }
    

    private XMLConfiguration getConfig() {
        try {
            return builder.getConfiguration();
        } catch (ConfigurationException e) {
            logger.error(e.getMessage());
            return new XMLConfiguration();
        }
    }

    /**
     * Gets the language data for the given iso-code 639-1 or 639-2B
     *
     * @param isoCode a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.connector.oai.model.language.Language} object.
     */
    public Language getLanguage(String isoCode) {
        SubnodeConfiguration languageConfig = null;
        try {
            if (isoCode.length() == 3) {
                languageConfig = (SubnodeConfiguration) getConfig().configurationsAt("language[iso_639-2=\"" + isoCode + "\"]")
                        .get(0);
            } else if (isoCode.length() == 2) {
                languageConfig = (SubnodeConfiguration) getConfig().configurationsAt("language[iso_639-1=\"" + isoCode + "\"]")
                        .get(0);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("No matching language found for " + isoCode);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
        if (languageConfig == null) {
            throw new IllegalArgumentException("No matching language found for " + isoCode);
        }
        Language language = new Language();
        language.setIsoCode(languageConfig.getString("iso_639-2"));
        language.setIsoCodeOld(languageConfig.getString("iso_639-1"));
        language.setEnglishName(languageConfig.getString("eng"));
        language.setGermanName(languageConfig.getString("ger"));
        language.setFrenchName(languageConfig.getString("fre"));

        return language;
    }

}

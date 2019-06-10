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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractTest;
import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.model.FieldConfiguration;
import io.goobi.viewer.connector.oai.model.LicenseType;
import io.goobi.viewer.connector.oai.model.Set;
import io.goobi.viewer.connector.utils.Configuration;
import io.goobi.viewer.connector.utils.SolrConstants;

public class ConfigurationTest extends AbstractTest {

    /**
     * @see Configuration#getAdditionalDocstructTypes()
     * @verifies return all values
     */
    @Test
    public void getAdditionalDocstructTypes_shouldReturnAllValues() throws Exception {
        List<String> values = DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes();
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("Article", values.get(0));
        Assert.assertEquals("Photograph", values.get(1));
    }

    /**
     * @see Configuration#getAdditionalSets()
     * @verifies return all values
     */
    @Test
    public void getAdditionalSets_shouldReturnAllValues() throws Exception {
        List<Set> values = DataManager.getInstance().getConfiguration().getAdditionalSets();
        Assert.assertNotNull(values);
        Assert.assertEquals(1, values.size());
        Set set = values.get(0);
        Assert.assertEquals("test", set.getSetName());
        Assert.assertEquals("testspec", set.getSetSpec());
        Assert.assertEquals("DC:a.b.c.d", set.getSetQuery());
    }

    /**
     * @see Configuration#getCollectionBlacklist()
     * @verifies return all key-value pairs
     */
    @Test
    public void getCollectionBlacklist_shouldReturnAllKeyvaluePairs() throws Exception {
        List<String> values = DataManager.getInstance().getConfiguration().getCollectionBlacklist();
        Assert.assertNotNull(values);
        Assert.assertEquals(4, values.size());
        Assert.assertEquals("DC:collection1", values.get(0));
        Assert.assertEquals("DC:collection2", values.get(1));
        Assert.assertEquals("MD_OTHERCOLLECTION:collection3", values.get(2));
        Assert.assertEquals("MD_OTHERCOLLECTION:collection4", values.get(3));
    }

    /**
     * @see Configuration#getCollectionBlacklistFilterSuffix()
     * @verifies construct suffix correctly
     */
    @Test
    public void getCollectionBlacklistFilterSuffix_shouldConstructSuffixCorrectly() throws Exception {
        Assert.assertEquals(" -DC:collection1 -DC:collection2 -MD_OTHERCOLLECTION:collection3 -MD_OTHERCOLLECTION:collection4",
                DataManager.getInstance().getConfiguration().getCollectionBlacklistFilterSuffix());
    }

    /**
     * @see Configuration#getDocumentResolverUrl()
     * @verifies return correct value
     */
    @Test
    public void getDocumentResolverUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost/viewer/metsresolver?id=", DataManager.getInstance().getConfiguration().getDocumentResolverUrl());
    }

    /**
     * @see Configuration#getEseDataProviderField()
     * @verifies return correct value
     */
    @Test
    public void getEseDataProviderField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("MD_DEFAULTPROVIDER", DataManager.getInstance().getConfiguration().getEseDataProviderField());
    }

    /**
     * @see Configuration#getEseDefaultProvider()
     * @verifies return correct value
     */
    @Test
    public void getEseDefaultProvider_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("Institution XYZ", DataManager.getInstance().getConfiguration().getEseDefaultProvider());
    }

    /**
     * @see Configuration#getEseDefaultRightsUrl()
     * @verifies return correct value
     */
    @Test
    public void getEseDefaultRightsUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://www.example.com/rights", DataManager.getInstance().getConfiguration().getEseDefaultRightsUrl());
    }

    /**
     * @see Configuration#getEseProviderField()
     * @verifies return correct value
     */
    @Test
    public void getEseProviderField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("MD_ACCESSCONDITIONCOPYRIGHT", DataManager.getInstance().getConfiguration().getEseProviderField());
    }

    /**
     * @see Configuration#getEseRightsField()
     * @verifies return correct value
     */
    @Test
    public void getEseRightsField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("TODO", DataManager.getInstance().getConfiguration().getEseRightsField());
    }

    /**
     * @see Configuration#getEseTypes()
     * @verifies return all values
     */
    @Test
    public void getEseTypes_shouldReturnAllValues() throws Exception {
        Map<String, String> values = DataManager.getInstance().getConfiguration().getEseTypes();
        Assert.assertNotNull(values);
        Assert.assertEquals(6, values.size());
        Assert.assertEquals("VIDEO", values.get("video"));
    }

    /**
     * @see Configuration#getIndexUrl()
     * @verifies return correct value
     */
    @Test
    public void getIndexUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost:8080/solr", DataManager.getInstance().getConfiguration().getIndexUrl());
    }

    /**
     * @see Configuration#getMods2MarcXsl()
     * @verifies return correct value
     */
    @Test
    public void getMods2MarcXsl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("resources/test/MODS2MARC21slim.xsl", DataManager.getInstance().getConfiguration().getMods2MarcXsl());
    }

    /**
     * @see Configuration#getPiResolverUrl()
     * @verifies return correct value
     */
    @Test
    public void getPiResolverUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost/viewer/piresolver?id=", DataManager.getInstance().getConfiguration().getPiResolverUrl());
    }

    /**
     * @see Configuration#getRestrictedAccessConditions()
     * @verifies return all values
     */
    @Test
    public void getRestrictedAccessConditions_shouldReturnAllValues() throws Exception {
        List<LicenseType> values = DataManager.getInstance().getConfiguration().getRestrictedAccessConditions();
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("MD_SOMEFIELD", values.get(0).getField());
        Assert.assertEquals("restricted1", values.get(0).getValue());
        Assert.assertNull(values.get(0).getConditions());
        Assert.assertEquals(SolrConstants.ACCESSCONDITION, values.get(1).getField());
        Assert.assertEquals("restricted2", values.get(1).getValue());
        Assert.assertEquals("-MDNUM_PUBLICRELEASEYEAR:[* TO NOW/YEAR]", values.get(1).getConditions());
    }

    /**
     * @see Configuration#getOaiFolder()
     * @verifies return correct value
     */
    @Test
    public void getOaiFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("resources/test/oai/", DataManager.getInstance().getConfiguration().getOaiFolder());
    }

    /**
     * @see Configuration#getResumptionTokenFolder()
     * @verifies return correct value
     */
    @Test
    public void getResumptionTokenFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("resources/test/oai/token/", DataManager.getInstance().getConfiguration().getResumptionTokenFolder());
    }

    /**
     * @see Configuration#getUrnResolverUrl()
     * @verifies return correct value
     */
    @Test
    public void getUrnResolverUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost/viewer/resolver?urn=", DataManager.getInstance().getConfiguration().getUrnResolverUrl());
    }

    /**
     * @see Configuration#getViewerConfigFolder()
     * @verifies return correct value
     */
    @Test
    public void getViewerConfigFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("resources/test/", DataManager.getInstance().getConfiguration().getViewerConfigFolder());
    }

    /**
     * @see Configuration#isUseCollectionBlacklist()
     * @verifies return correct value
     */
    @Test
    public void isUseCollectionBlacklist_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isUseCollectionBlacklist());
    }

    /**
     * @see Configuration#getUrnPrefixBlacklist()
     * @verifies return all values
     */
    @Test
    public void getUrnPrefixBlacklist_shouldReturnAllValues() throws Exception {
        List<String> values = DataManager.getInstance().getConfiguration().getUrnPrefixBlacklist();
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("urn:nbn:de:test_", values.get(0));
        Assert.assertEquals("urn:nbn:de:hidden_", values.get(1));
    }

    /**
     * @see Configuration#getHitsPerToken()
     * @verifies return correct value
     */
    @Test
    public void getHitsPerToken_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(23, DataManager.getInstance().getConfiguration().getHitsPerToken());
    }

    /**
     * @see Configuration#getHitsPerTokenForMetadataFormat(String)
     * @verifies return correct value
     */
    @Test
    public void getHitsPerTokenForMetadataFormat_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(11, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.oai_dc.name()));
        Assert.assertEquals(12, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.ese.name()));
        Assert.assertEquals(13, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.mets.name()));
        Assert.assertEquals(14, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.lido.name()));
        Assert.assertEquals(15, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.marcxml.name()));
        Assert.assertEquals(16, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.epicur.name()));
        Assert.assertEquals(17, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.iv_overviewpage.name()));
        Assert.assertEquals(18, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.iv_crowdsourcing.name()));
    }

    /**
     * @see Configuration#getHitsPerTokenForMetadataFormat(String)
     * @verifies return default value for unknown formats
     */
    @Test
    public void getHitsPerTokenForMetadataFormat_shouldReturnDefaultValueForUnknownFormats() throws Exception {
        Assert.assertEquals(23, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat("notfound"));
    }

    /**
     * @see Configuration#getVersionDisriminatorFieldForMetadataFormat(String)
     * @verifies return correct value
     */
    @Test
    public void getVersionDisriminatorFieldForMetadataFormat_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(SolrConstants.LANGUAGE, DataManager.getInstance().getConfiguration().getVersionDisriminatorFieldForMetadataFormat("tei"));
    }

    /**
     * @see Configuration#isMetadataFormatEnabled(String)
     * @verifies return correct value
     */
    @Test
    public void isMetadataFormatEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.oai_dc.name()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.ese.name()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.mets.name()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.lido.name()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.marcxml.name()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.epicur.name()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.iv_overviewpage.name()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.iv_crowdsourcing.name()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.tei.name()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.cmdi.name()));
    }

    /**
     * @see Configuration#isMetadataFormatEnabled(String)
     * @verifies return false for unknown formats
     */
    @Test
    public void isMetadataFormatEnabled_shouldReturnFalseForUnknownFormats() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled("notfound"));
    }

    /**
     * @see Configuration#getAllValuesSets()
     * @verifies return all values
     */
    @Test
    public void getAllValuesSets_shouldReturnAllValues() throws Exception {
        List<Set> values = DataManager.getInstance().getConfiguration().getAllValuesSets();
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("DC", values.get(0).getSetName());
        Assert.assertTrue(values.get(0).isTranslate());
        Assert.assertEquals("MD_WISSENSGEBIET", values.get(1).getSetName());
        Assert.assertFalse(values.get(1).isTranslate());
    }

    /**
     * @see Configuration#getQuerySuffix()
     * @verifies return correct value
     */
    @Test
    public void getQuerySuffix_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("-PI:PPN123456789", DataManager.getInstance().getConfiguration().getQuerySuffix());
    }

    /**
     * @see Configuration#getFieldForMetadataFormat(String)
     * @verifies return all values
     */
    @Test
    public void getFieldForMetadataFormat_shouldReturnAllValues() throws Exception {
        List<FieldConfiguration> values = DataManager.getInstance().getConfiguration().getFieldForMetadataFormat(Metadata.oai_dc.name());
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());

        Assert.assertEquals("title", values.get(0).getFieldName());
        Assert.assertEquals("MD_TITLE", values.get(0).getValueSource());
        Assert.assertTrue(values.get(0).isTranslate());
        Assert.assertTrue(values.get(0).isMultivalued());
        Assert.assertTrue(values.get(0).isUseTopstructValueIfNoneFound());
        Assert.assertEquals("", values.get(0).getDefaultValue());

        Assert.assertEquals("format", values.get(1).getFieldName());
        Assert.assertNull(values.get(1).getValueSource());
        Assert.assertEquals("image/jpeg", values.get(1).getDefaultValue());
        Assert.assertEquals("pre ", values.get(1).getPrefix());
        Assert.assertEquals(" suf", values.get(1).getSuffix());
    }

    /**
     * @see Configuration#getBaseURL()
     * @verifies return correct value
     */
    @Test
    public void getBaseURL_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost:8080/viewer/oai", DataManager.getInstance().getConfiguration().getBaseURL());
    }

    /**
     * @see Configuration#isBaseUrlUseInRequestElement()
     * @verifies return correct value
     */
    @Test
    public void isBaseUrlUseInRequestElement_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isBaseUrlUseInRequestElement());
    }

    /**
     * @see Configuration#getDefaultLocale()
     * @verifies return correct value
     */
    @Test
    public void getDefaultLocale_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(Locale.GERMAN, DataManager.getInstance().getConfiguration().getDefaultLocale());
    }

    /**
     * @see Configuration#getSetSpecFieldsForMetadataFormat(String)
     * @verifies return all values
     */
    @Test
    public void getSetSpecFieldsForMetadataFormat_shouldReturnAllValues() throws Exception {
        List<String> values = DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.oai_dc.name());
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals(SolrConstants.DC, values.get(0));
        Assert.assertEquals(SolrConstants.DOCSTRCT, values.get(1));
    }

    /**
     * @see Configuration#getContentApiUrl()
     * @verifies return correct value
     */
    @Test
    public void getContentApiUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost/viewer/rest/content/", DataManager.getInstance().getConfiguration().getContentApiUrl());
    }

    /**
     * @see Configuration#getHarvestUrl()
     * @verifies return correct value
     */
    @Test
    public void getHarvestUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost/viewer/harvest", DataManager.getInstance().getConfiguration().getHarvestUrl());
    }

    /**
     * @see Configuration#getFulltextUrl()
     * @verifies return correct value
     */
    @Test
    public void getFulltextUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost/viewer/rest/content/fulltext/{pi}/{page}/",
                DataManager.getInstance().getConfiguration().getFulltextUrl());
    }
}
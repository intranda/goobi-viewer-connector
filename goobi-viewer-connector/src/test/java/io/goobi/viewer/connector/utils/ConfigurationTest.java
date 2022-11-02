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
import io.goobi.viewer.connector.oai.model.Set;
import io.goobi.viewer.connector.oai.model.formats.Format;

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
        Assert.assertEquals("src/test/resources/MODS2MARC21slim.xsl", DataManager.getInstance().getConfiguration().getMods2MarcXsl());
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
     * @see Configuration#getOaiFolder()
     * @verifies return correct value
     */
    @Test
    public void getOaiFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("src/test/resources/oai/", DataManager.getInstance().getConfiguration().getOaiFolder());
    }

    /**
     * @see Configuration#getResumptionTokenFolder()
     * @verifies return correct value
     */
    @Test
    public void getResumptionTokenFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("src/test/resources/oai/token/", DataManager.getInstance().getConfiguration().getResumptionTokenFolder());
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
        Assert.assertEquals("src/test/resources/", DataManager.getInstance().getConfiguration().getViewerConfigFolder());
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
        Assert.assertEquals(11, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.OAI_DC.getMetadataPrefix()));
        Assert.assertEquals(12, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.ESE.name().toLowerCase()));
        Assert.assertEquals(13, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.METS.getMetadataPrefix()));
        Assert.assertEquals(14, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.LIDO.getMetadataPrefix()));
        Assert.assertEquals(15, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.MARCXML.getMetadataPrefix()));
        Assert.assertEquals(16, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.EPICUR.getMetadataPrefix()));
        Assert.assertEquals(17,
                DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.IV_OVERVIEWPAGE.getMetadataPrefix()));
        Assert.assertEquals(18,
                DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.IV_CROWDSOURCING.getMetadataPrefix()));
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
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.OAI_DC.getMetadataPrefix()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.ESE.name().toLowerCase()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.METS.getMetadataPrefix()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.LIDO.getMetadataPrefix()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.MARCXML.getMetadataPrefix()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.EPICUR.getMetadataPrefix()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.IV_OVERVIEWPAGE.getMetadataPrefix()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.IV_CROWDSOURCING.getMetadataPrefix()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.TEI.getMetadataPrefix()));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.CMDI.getMetadataPrefix()));
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
        Assert.assertFalse(values.get(0).isTranslate());
        Assert.assertEquals("MD_WISSENSGEBIET", values.get(1).getSetName());
        Assert.assertFalse(values.get(1).isTranslate());
    }

    /**
     * @see Configuration#getFieldForMetadataFormat(String)
     * @verifies return all values
     */
    @Test
    public void getFieldForMetadataFormat_shouldReturnAllValues() throws Exception {
        List<FieldConfiguration> values = DataManager.getInstance().getConfiguration().getFieldForMetadataFormat(Metadata.OAI_DC.getMetadataPrefix());
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
        List<String> values = DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.OAI_DC.getMetadataPrefix());
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals(SolrConstants.DC, values.get(0));
        Assert.assertEquals(SolrConstants.DOCSTRCT, values.get(1));
    }

    /**
     * @see Configuration#getRestApiUrl()
     * @verifies return correct value
     */
    @Test
    public void getRestApiUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost/viewer/rest/", DataManager.getInstance().getConfiguration().getRestApiUrl());
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

    /**
     * @see Configuration#getAccessConditionMappingForMetadataFormat(String,String)
     * @verifies return correct value
     */
    @Test
    public void getAccessConditionMappingForMetadataFormat_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(Format.ACCESSCONDITION_OPENACCESS,
                DataManager.getInstance()
                        .getConfiguration()
                        .getAccessConditionMappingForMetadataFormat(Metadata.OAI_DC.getMetadataPrefix(), "Public Domain Mark 1.0"));
        Assert.assertEquals(Format.ACCESSCONDITION_OPENACCESS,
                DataManager.getInstance()
                        .getConfiguration()
                        .getAccessConditionMappingForMetadataFormat(Metadata.OAI_DC.getMetadataPrefix(), "Rechte vorbehalten - Freier Zugang"));
    }

    /**
     * @see Configuration#getOaiIdentifier()
     * @verifies read config values correctly
     */
    @Test
    public void getOaiIdentifier_shouldReadConfigValuesCorrectly() throws Exception {
        Map<String, String> map = DataManager.getInstance().getConfiguration().getOaiIdentifier();
        Assert.assertEquals("http://www.openarchives.org/OAI/2.0/", map.get("xmlns"));
        Assert.assertEquals("repo", map.get("repositoryIdentifier"));
    }

}

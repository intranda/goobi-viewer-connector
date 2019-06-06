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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.lang.StringUtils;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.connector.oai.model.FieldConfiguration;
import io.goobi.viewer.connector.oai.model.LicenseType;
import io.goobi.viewer.connector.oai.model.Set;
import io.goobi.viewer.connector.oai.model.metadata.Metadata;
import io.goobi.viewer.connector.oai.model.metadata.MetadataParameter;
import io.goobi.viewer.connector.oai.model.metadata.MetadataParameter.MetadataParameterType;

public final class Configuration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    public static final String DEFAULT_CONFIG_FILE = "config_oai.xml";
    private static final String DEFAULT_VIEWER_CONFIG_FILE = "config_viewer.xml";

    private XMLConfiguration configLocal = null;
    private XMLConfiguration configDefault = null;
    private XMLConfiguration viewerConfig = null;

    public Configuration(String configPath) {
        // Load default configuration
        try {
            configDefault = new XMLConfiguration(configPath);
            configDefault.setReloadingStrategy(new FileChangedReloadingStrategy());
            logger.info("Loaded default Connector configuration.");
        } catch (ConfigurationException e) {
            logger.error(e.getMessage(), e);
        }
        // Load local configuration
        try {
            configLocal = new XMLConfiguration(getViewerConfigFolder() + DEFAULT_CONFIG_FILE);
            configLocal.setReloadingStrategy(new FileChangedReloadingStrategy());
            logger.info("Loaded local Connector configuration from '{}'.", configLocal.getFile().getAbsolutePath());
        } catch (ConfigurationException e) {
            logger.warn("Local OAI configuration file '{}' could not be read ({}), using default configuration file.", getViewerConfigFolder(),
                    e.getMessage());
            configLocal = configDefault;
        }
        // Load viewer configuration
        try {
            viewerConfig = new XMLConfiguration(getViewerConfigFolder() + DEFAULT_VIEWER_CONFIG_FILE);
            viewerConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
            logger.info("Loaded default Goobi viewer configuration for Connector.");
        } catch (ConfigurationException e) {
            logger.error(e.getMessage(), e);
        }
    }


    /**
     * ns is needed as parameter for every xml element, otherwise the standard-ns is printed out in every element the standard return String =
     * http://www.openarchives.org/OAI/2.0/
     * 
     * @return the Standard Namespace for the xml response
     */
    public Namespace getStandardNameSpace() {
        String xmlns = getOaiIdentifier().get("xmlns");
        return Namespace.getNamespace(xmlns);
    }

    /**
     * 
     * @param inPath
     * @return
     */
    private List<HierarchicalConfiguration> getLocalConfigurationsAt(String inPath) {
        List<HierarchicalConfiguration> ret = configLocal.configurationsAt(inPath);
        if (ret == null || ret.isEmpty()) {
            ret = configDefault.configurationsAt(inPath);
        }

        return ret;
    }

    /**
     * 
     * @param inPath
     * @param defaultList
     * @return
     */
    private List<String> getLocalList(String inPath, List<String> defaultList) {
        return getLocalList(configLocal, configDefault, inPath, defaultList);
    }

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return
     */
    private boolean getLocalBoolean(String inPath, boolean inDefault) {
        try {
            return configLocal.getBoolean(inPath, configDefault.getBoolean(inPath, inDefault));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return inDefault;
        }
    }

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return
     */
    private int getLocalInt(String inPath, int inDefault) {
        try {
            return configLocal.getInt(inPath, configDefault.getInt(inPath, inDefault));
        } catch (ConversionException e) {
            logger.error("{}. Using default value {} instead.", e.getMessage(), inDefault);
            return inDefault;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return inDefault;
        }
    }

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return
     */
    private String getLocalString(String inPath, String inDefault) {
        try {
            return configLocal.getString(inPath, configDefault.getString(inPath, inDefault));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return inDefault;
        }
    }

    /**
     * 
     * @param config Preferred configuration
     * @param altConfig Alternative configuration
     * @param inPath XML path
     * @param defaultList List of default values to return if none found in config
     * @return
     */
    private static List<String> getLocalList(HierarchicalConfiguration config, HierarchicalConfiguration altConfig, String inPath,
            List<String> defaultList) {
        if (config == null) {
            throw new IllegalArgumentException("config may not be null");
        }
        List<Object> objects = config.getList(inPath, altConfig == null ? null : altConfig.getList(inPath, defaultList));
        if (objects != null && !objects.isEmpty()) {
            List<String> ret = new ArrayList<>(objects.size());
            for (Object obj : objects) {
                ret.add((String) obj);
            }
            return ret;
        }

        return new ArrayList<>(0);
    }

    /**
     * this method returns a HashMap with information for the oai header and identify verb
     * 
     * @return {@link HashMap}
     */

    public Map<String, String> getIdentifyTags() {
        Map<String, String> identifyTags = new HashMap<>();

        identifyTags.put("repositoryName", getLocalString("identifyTags.repositoryName", null));
        identifyTags.put("baseURL", getLocalString("identifyTags.baseURL", null));
        identifyTags.put("protocolVersion", getLocalString("identifyTags.protocolVersion", null));
        identifyTags.put("adminEmail", getLocalString("identifyTags.adminEmail", null));
        identifyTags.put("deletedRecord", getLocalString("identifyTags.deletedRecord", null));
        identifyTags.put("granularity", getLocalString("identifyTags.granularity", null));

        return identifyTags;
    }

    /**
     * This method generates a HashMap with information for oai header
     * 
     * @return {@link HashMap}
     */
    public Map<String, String> getOaiIdentifier() {
        Map<String, String> oaiIdentifier = new HashMap<>();
        oaiIdentifier.put("xmlns", getLocalString("oai-identifier.xmlns", null));
        oaiIdentifier.put("repositoryIdentifier", getLocalString("oai-identifier.repositoryIdentifier", null));

        return oaiIdentifier;
    }

    /**
     * 
     * @return Configured viewerConfigFolder in the default config file; /opt/digiverso/viewer/config/ if no value configured
     * @should return correct value
     */
    public String getViewerConfigFolder() {
        
        String configLocalPath = configDefault.getString("viewerConfigFolder", "/opt/digiverso/viewer/config/");
        if (!configLocalPath.endsWith("/")) {
            configLocalPath += "/";
        }
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0 && configLocalPath.startsWith("/opt/")) {
            configLocalPath = configLocalPath.replace("/opt", "C:");
        }
        return configLocalPath;        
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getOaiFolder() {
        return getLocalString("oaiFolder", "/opt/digiverso/viewer/oai/");
    }

    /**
     * 
     * @return folder for resumptionToken
     * @should return correct value
     */
    public String getResumptionTokenFolder() {
        return getLocalString("resumptionTokenFolder", "/opt/digiverso/viewer/oai/token/");
    }

    /**
     * 
     * @return where to find the index
     * @should return correct value
     */
    public String getIndexUrl() {
        return getLocalString("solr.solrUrl", "http://localhost:8080/solr");
    }

    /**
     * 
     * @return number of hits per page/token
     * @should return correct value
     */
    public int getHitsPerToken() {
        return getLocalInt("solr.hitsPerToken", 20);
    }

    /**
     * 
     * @return number of hits per page/token
     * @should return correct value
     * @should return default value for unknown formats
     */
    public int getHitsPerTokenForMetadataFormat(String metadataFormat) {
        return getLocalInt(metadataFormat + ".hitsPerToken", getHitsPerToken());
    }

    /**
     * 
     * @return number of hits per page/token
     * @should return correct value
     */
    public String getVersionDisriminatorFieldForMetadataFormat(String metadataFormat) {
        return getLocalString(metadataFormat + ".versionDiscriminatorField", null);
    }

    /**
     * 
     * @return number of hits per page/token
     * @should return correct value
     * @should return false for unknown formats
     */
    public boolean isMetadataFormatEnabled(String metadataFormat) {
        return getLocalBoolean(metadataFormat + ".enabled", false);
    }

    /**
     * 
     * @param metadataFormat
     * @return
     * @should return all values
     */
    public List<FieldConfiguration> getFieldForMetadataFormat(String metadataFormat) {
        if (metadataFormat == null) {
            throw new IllegalArgumentException("metadataFormat may not be null");
        }

        List<HierarchicalConfiguration> elements = getLocalConfigurationsAt(metadataFormat + ".fields.field");
        if (elements != null) {
            List<FieldConfiguration> ret = new ArrayList<>(elements.size());
            for (Iterator<HierarchicalConfiguration> it = elements.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = it.next();
                Map<String, String> fieldConfig = new HashMap<>();
                String name = sub.getString("[@name]", null);
                String valueSource = sub.getString("[@valueSource]", null);
                boolean translate = sub.getBoolean("[@translate]", false);
                boolean multivalued = sub.getBoolean("[@multivalued]", false);
                boolean useTopstructValueIfNoneFound = sub.getBoolean("[@useTopstructValueIfNoneFound]", false);
                String defaultValue = sub.getString("[@defaultValue]", null);
                String prefix = sub.getString("[@prefix]", null);
                if (prefix != null) {
                    prefix = prefix.replace("_SPACE_", " ");
                }
                String suffix = sub.getString("[@suffix]", null);
                if (suffix != null) {
                    suffix = suffix.replace("_SPACE_", " ");
                }
                ret.add(new FieldConfiguration(name, valueSource, translate, multivalued, useTopstructValueIfNoneFound, defaultValue, prefix,
                        suffix));
            }
            return ret;
        }

        return null;
    }

    /**
     * 
     * @param metadataFormat
     * @return
     * @should return all values
     */
    public List<String> getSetSpecFieldsForMetadataFormat(String metadataFormat) {
        if (metadataFormat == null) {
            throw new IllegalArgumentException("metadataFormat may not be null");
        }

        return getLocalList(metadataFormat + ".setSpec.field", new ArrayList<String>(0));
    }

    /**
     * @return Harvest servlet URL
     * @should return correct value
     * 
     */
    public String getHarvestUrl() {
        return getLocalString("harvestUrl", "http://localhost:8080/viewer/harvest");
    }

    /**
     * @return Content file REST API URL
     * @should return correct value
     */
    public String getContentApiUrl() {
        return getLocalString("contentApiUrl", "http://localhost:8080/viewer/rest/content/");
    }

    /**
     * @return Full-text file URL with placeholders for PI and page.
     * @should return correct value
     */
    public String getFulltextUrl() {
        return getLocalString("fulltextUrl", "http://localhost:8080/viewer/rest/fulltext/{pi}/{page}/");
    }

    /**
     * Returns mappings for the ESE "type" element.
     * 
     * @return
     * @should return all values
     */
    @SuppressWarnings("rawtypes")
    public Map<String, String> getEseTypes() {
        Map<String, String> ret = new HashMap<>();

        List<HierarchicalConfiguration> types = getLocalConfigurationsAt("ese.types.docstruct");
        if (types != null) {
            for (Iterator it = types.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();
                ret.put(sub.getString("[@name]"), sub.getString("[@type]"));
            }
        }

        return ret;
    }

    /**
     * @return Path to the MODS2MARC XSLT stylesheet.
     * @should return correct value
     */
    public String getMods2MarcXsl() {
        return getLocalString("marcxml.marcStylesheet", null);
    }

    /**
     * 
     * @return URN resolver URL.
     * @should return correct value
     */
    public String getUrnResolverUrl() {
        return getLocalString("urnResolverUrl", "http://localhost:8080/viewer/resolver?urn=");
    }

    /**
     * 
     * @return PI resolver URL.
     * @should return correct value
     */
    public String getPiResolverUrl() {
        return getLocalString("piResolverUrl", "http://localhost:8080/viewer/piresolver?id=");
    }

    /**
     * 
     * @return METS/LIDO resolver URL.
     * @should return correct value
     */
    public String getDocumentResolverUrl() {
        return getLocalString("documentResolverUrl", "http://localhost:8080/viewer/metsresolver?id=");
    }

    /**
     * Returns a list of additional docstruct types "type" element.
     * 
     * @return
     * @should return all values
     */
    @SuppressWarnings("rawtypes")
    public List<String> getAdditionalDocstructTypes() {
        List<String> ret = new ArrayList<>();

        List<HierarchicalConfiguration> docstructs = getLocalConfigurationsAt("epicur.additionalDocstructTypes.docstruct");
        if (docstructs != null) {
            for (Iterator it = docstructs.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();
                ret.add(sub.getString("."));
                logger.trace("loaded additional docstruct type: {}", sub.getString("."));
            }
        }

        return ret;
    }

    /**
     * 
     * @return
     * @should return all values
     */
    public List<String> getUrnPrefixBlacklist() {
        return getLocalList("epicur.blacklist.urnPrefix", new ArrayList<String>(0));
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getEseProviderField() {
        return getLocalString("ese.providerField", null);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getEseDataProviderField() {
        return getLocalString("ese.dataProviderField", null);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getEseDefaultProvider() {
        return getLocalString("ese.defaultProvider", null);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getEseRightsField() {
        return getLocalString("ese.rightsField", null);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getEseDefaultRightsUrl() {
        return getLocalString("ese.defaultRightsUrl", null);
    }

    /**
     * 
     * @return
     * @should return all values
     */
    public List<Set> getAllValuesSets() {
        List<HierarchicalConfiguration> types = getLocalConfigurationsAt("sets.allValuesSet");
        if (types != null) {
            List<Set> ret = new ArrayList<>(types.size());
            for (Iterator<HierarchicalConfiguration> it = types.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = it.next();
                Set set = new Set(sub.getString("."), null, null);
                set.setTranslate(sub.getBoolean("[@translate]", false));
                ret.add(set);
            }
            return ret;
        }
        return new ArrayList<>(0);
    }

    /**
     * 
     * @return
     * @should return all values
     */
    public List<Set> getAdditionalSets() {
        List<HierarchicalConfiguration> types = getLocalConfigurationsAt("sets.set");
        if (types != null) {
            List<Set> ret = new ArrayList<>(types.size());
            for (Iterator<HierarchicalConfiguration> it = types.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = it.next();
                Set set = new Set(sub.getString("[@setName]"), sub.getString("[@setSpec]"), sub.getString("[@setQuery]"));
                ret.add(set);
            }
            return ret;
        }
        return new ArrayList<>(0);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getBaseURL() {
        return getLocalString("identifyTags.baseURL", null);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isBaseUrlUseInRequestElement() {
        return getLocalBoolean("identifyTags.baseURL[@useInRequestElement]", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isUseCollectionBlacklist() {
        return getLocalBoolean("useCollectionBlacklist", true);
    }

    /**
     * Returns collection names to be omitted from search results, listings etc. from config_viewer.xml
     * 
     * @return
     * @should return all key-value pairs
     */
    public List<String> getCollectionBlacklist() {
        List<String> ret = new ArrayList<>();
        if (viewerConfig == null) {
            logger.error("Viewer config not loaded, cannot read collection blacklist.");
            return ret;
        }
        List<HierarchicalConfiguration> templates = viewerConfig.configurationsAt("collections.collection");
        if (templates != null && !templates.isEmpty()) {
            for (Iterator<HierarchicalConfiguration> it = templates.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = it.next();
                String field = sub.getString("[@field]");
                List<Object> collections = sub.getList("blacklist.collection");
                for (Object o : collections) {
                    ret.add(field + ":" + (String) o);
                }
            }
        }
        return ret;
    }

    /**
     * 
     * @return
     * @should construct suffix correctly
     * 
     */
    public String getCollectionBlacklistFilterSuffix() {
        StringBuilder sbQuery = new StringBuilder();
        List<String> list = getCollectionBlacklist();
        if (!list.isEmpty()) {
            for (String s : list) {
                if (StringUtils.isNotBlank(s)) {
                    sbQuery.append(" -").append(s.trim());
                }
            }
        }

        return sbQuery.toString();
    }

    /**
     * @return
     * @should return all values
     */
    public List<LicenseType> getRestrictedAccessConditions() {
        List<HierarchicalConfiguration> elements = getLocalConfigurationsAt("solr.restrictions.restriction");
        if (elements != null) {
            List<LicenseType> ret = new ArrayList<>(elements.size());
            for (Iterator<HierarchicalConfiguration> it = elements.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = it.next();
                Map<String, String> fieldConfig = new HashMap<>();
                String value = sub.getString(".");
                String field = sub.getString("[@field]", null);
                String condition = sub.getString("[@conditions]", null);
                ret.add(new LicenseType(field, value, condition));
            }
            return ret;
        }

        return new ArrayList<>(0);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getQuerySuffix() {
        return getLocalString("solr.querySuffix", "");
    }

    public String getLocalRessourceBundleFile() {
        return getOaiFolder() + "messages_de.properties";
    }

    /**
     * 
     * @return
     * @should return correct value
     * @should return English if locale not found
     */
    public Locale getDefaultLocale() {
        String language = getLocalString("defaultLocale", "en");
        Locale locale = Locale.forLanguageTag(language);
        if (locale == null) {
            locale = Locale.ENGLISH;
        }

        return locale;
    }

    /**
     * @param metadataFormat
     * @param template
     * @return
     * @should return correct template configuration
     * @should return default template configuration if template not found
     */
    @SuppressWarnings({ "rawtypes" })
    public List<Metadata> getMetadataConfiguration(String metadataFormat, String template) {
        HierarchicalConfiguration usingTemplate = null;
        List templateList = getLocalConfigurationsAt(metadataFormat + ".fields.template");
        if (templateList != null) {
            HierarchicalConfiguration defaultTemplate = null;

            for (Iterator it = templateList.iterator(); it.hasNext();) {
                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
                if (subElement.getString("[@name]").equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if ("_DEFAULT".equals(subElement.getString("[@name]"))) {
                    defaultTemplate = subElement;
                }
            }

            // If the requested template does not exist in the config, use _DEFAULT
            if (usingTemplate == null) {
                usingTemplate = defaultTemplate;
            }

        }

        return getMetadataForTemplate(usingTemplate);
    }

    /**
     * Reads metadata configuration for the given template configuration item. Returns empty list if template is null.
     * 
     * @param templateList
     * @param template
     * @return
     */
    @SuppressWarnings("rawtypes")
    private static List<Metadata> getMetadataForTemplate(HierarchicalConfiguration usingTemplate) {
        List<Metadata> ret = new ArrayList<>();

        if (usingTemplate != null) {
            List elements = usingTemplate.configurationsAt("metadata");
            if (elements != null) {
                for (Iterator it2 = elements.iterator(); it2.hasNext();) {
                    HierarchicalConfiguration sub = (HierarchicalConfiguration) it2.next();
                    String label = sub.getString("[@label]");
                    String masterValue = sub.getString("[@value]");
                    boolean group = sub.getBoolean("[@group]", false);
                    boolean multivalued = sub.getBoolean("[@multivalued]", false);
                    int number = sub.getInt("[@number]", -1);
                    int type = sub.getInt("[@type]", 0);
                    List params = sub.configurationsAt("param");
                    List<MetadataParameter> paramList = null;
                    if (params != null) {
                        paramList = new ArrayList<>(params.size());
                        for (Iterator it3 = params.iterator(); it3.hasNext();) {
                            HierarchicalConfiguration sub2 = (HierarchicalConfiguration) it3.next();
                            String fieldType = sub2.getString("[@type]");
                            String source = sub2.getString("[@source]", null);
                            String key = sub2.getString("[@key]");
                            String overrideMasterValue = sub2.getString("[@value]");
                            String defaultValue = sub2.getString("[@defaultValue]");
                            String prefix = sub2.getString("[@prefix]", "").replace("_SPACE_", " ");
                            String suffix = sub2.getString("[@suffix]", "").replace("_SPACE_", " ");
                            boolean addUrl = sub2.getBoolean("[@url]", false);
                            boolean dontUseTopstructValue = sub2.getBoolean("[@dontUseTopstructValue]", false);
                            paramList.add(new MetadataParameter(MetadataParameterType.getByString(fieldType), source, key, overrideMasterValue,
                                    defaultValue, prefix, suffix, addUrl, dontUseTopstructValue));
                        }
                    }
                    ret.add(new Metadata(label, masterValue, type, paramList, group, number, multivalued));
                }
            }
        }

        return ret;
    }
}

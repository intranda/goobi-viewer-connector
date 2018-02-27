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
package de.intranda.digiverso.m2m.oai.enums;

public enum Metadata {
    mets("mets", "http://www.loc.gov/mets/mets.xsd", "http://www.loc.gov/METS/", true, true),
    mods("mods", "http://www.loc.gov/standards/mods/v3/mods-3-3.xsd", "http://www.loc.gov/mods/v3", false, true),
    solr("solr", "TODO", "TODO", false, true),
    marcxml("marcxml", "http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd", "http://www.loc.gov/MARC21/slim", true, true),
    oai_dc("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/", true, false),
    dc("dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://purl.org/dc/elements/1.1/", false, true),
    epicur("epicur", "http://nbn-resolving.de/urn/resolver.pl?urn=urn:nbn:de:1111-2004033116", "urn:nbn:de:1111-2004033116", true, false),
    lido("lido", "http://www.lido-schema.org/schema/v1.0/lido-v1.0.xsd", "http://www.lido-schema.org", true, true),
    ese("europeana", "http://www.europeana.eu/schemas/ese/ESE-V3.4.xsd", "http://www.europeana.eu/schemas/ese/", true, false),
    // TODO final namespaces
    iv_overviewpage("iv_overviewpage", "http://www.intranda.com/intrandaviewer_overviewpage.xsd",
            "http://www.intranda.com/digiverso/intrandaviewer/overviewpage", true, false),
    iv_crowdsourcing("iv_crowdsourcing", "http://www.intranda.com/intrandaviewer_overviewpage.xsd",
            "http://www.intranda.com/digiverso/intrandaviewer/crowdsourcing", true, false),
    tei("tei", "https://www.tei-c.org/release/xml/tei/custom/schema/xsd/tei_all.xsd", "http://www.tei-c.org/ns/1.0", true, false),
    cmdi("cmd",
            "http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1380106710826 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1380106710826/xsd",
            "http://www.clarin.eu/cmd/1", true, false);

    private String metadataPrefix;
    private String schema;
    private String metadataNamespace;
    private boolean oaiSet;
    private boolean sruSet;

    /**
     * Constructor.
     * 
     * @param metadataPrefix
     * @param schema
     * @param metadataNamespace
     * @param oaiSet Use format for SRU
     * @param sruSet Use format for OAI-PMH
     */
    private Metadata(String metadataPrefix, String schema, String metadataNamespace, boolean oaiSet, boolean sruSet) {
        this.metadataPrefix = metadataPrefix;
        this.schema = schema;
        this.metadataNamespace = metadataNamespace;
        this.oaiSet = oaiSet;
        this.sruSet = sruSet;
    }

    public String getMetadataNamespace() {
        return metadataNamespace;
    }

    public String getSchema() {
        return schema;
    }

    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    /**
     * @return the sruSet
     */
    public boolean isSruSet() {
        return sruSet;
    }

    /**
     * @return the oaiSet
     */
    public boolean isOaiSet() {
        return oaiSet;
    }

    public static Metadata getByMetadataPrefix(String metadataPrefix) {
        for (Metadata m : Metadata.values()) {
            if (m.getMetadataPrefix()
                    .equals(metadataPrefix)) {
                return m;
            }
        }
        return null;
    }
}

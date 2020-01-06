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
package io.goobi.viewer.connector.messages;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.connector.DataManager;

/**
 * <p>MessageResourceBundle class.</p>
 *
 */
public class MessageResourceBundle extends ResourceBundle {

    private static final Logger logger = LoggerFactory.getLogger(MessageResourceBundle.class);

    static ResourceBundle bundle = null;
    static ResourceBundle localBundle = null;

    /**
     * <p>loadResourceBundle.</p>
     *
     * @param inLocale a {@link java.util.Locale} object.
     */
    public static synchronized void loadResourceBundle(Locale inLocale) {
        Locale locale;
        if (inLocale != null) {
            locale = inLocale;
        } else {
            locale = Locale.ENGLISH;
        }
        if (bundle == null || !bundle.getLocale().equals(locale)) {
            bundle = ResourceBundle.getBundle("messages", locale);
        }
        if (localBundle == null || !localBundle.getLocale().equals(locale)) {
            localBundle = loadLocalResourceBundle(locale);
        }
    }

    private static ResourceBundle loadLocalResourceBundle(Locale locale) {
        File file = new File(DataManager.getInstance().getConfiguration().getLocalRessourceBundleFile());
        if (file.exists()) {
            try {
                URL resourceURL = file.getParentFile().toURI().toURL();
                logger.debug("Local resource bundle URL {}: ", file.getParentFile().toURI().toURL());
                URLClassLoader urlLoader = new URLClassLoader(new URL[] { resourceURL });
                return ResourceBundle.getBundle("messages", locale, urlLoader);
            } catch (Exception e) {
                // some error while loading bundle from file system; use
                // default bundle now ...
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected Object handleGetObject(String key) {
        return getTranslation(key, Locale.ENGLISH);
    }

    /**
     * <p>getTranslation.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param locale a {@link java.util.Locale} object.
     * @should translate text correctly
     * @return a {@link java.lang.String} object.
     */
    public static String getTranslation(String text, Locale locale) {
        loadResourceBundle(locale);
        return MessageResourceBundle.getTranslation(text, bundle, localBundle);
    }

    /**
     * <p>getTranslation.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param globalBundle a {@link java.util.ResourceBundle} object.
     * @param localBundle a {@link java.util.ResourceBundle} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getTranslation(String key, ResourceBundle globalBundle, ResourceBundle localBundle) {
        if (key != null) {
            // Remove trailing asterisk
            if (key.endsWith("*")) {
                key = key.substring(0, key.length() - 1);
            }

            if (localBundle != null) {
                if (localBundle.containsKey(key)) {
                    return localBundle.getString(key);
                }
            }
            // Remove leading SORT_
            if (key.startsWith("SORT_")) {
                String newKey = key.replace("SORT_", "");
                if (localBundle.containsKey("MD_" + newKey)) {
                    return localBundle.getString("MD_" + newKey);
                }
                if (localBundle.containsKey(newKey)) {
                    return localBundle.getString(newKey);
                }
            }
            // Remove leading FACET_
            if (key.startsWith("FACET_")) {
                String newKey = key.replace("FACET_", "");
                if (localBundle.containsKey("MD_" + newKey)) {
                    return localBundle.getString("MD_" + newKey);
                }
                if (localBundle.containsKey(newKey)) {
                    return localBundle.getString(newKey);
                }
            }
        }
        if (globalBundle != null) {
            try {
                if (globalBundle.containsKey(key)) {
                    return globalBundle.getString(key);
                }

                // Remove leading SORT_
                if (key.startsWith("SORT_")) {
                    String newKey = key.replace("SORT_", "");
                    if (globalBundle.containsKey("MD_" + newKey)) {
                        return globalBundle.getString("MD_" + newKey);
                    }
                    if (globalBundle.containsKey(newKey)) {
                        return globalBundle.getString(newKey);
                    }
                }
                // Remove leading FACET_
                if (key.startsWith("FACET_")) {
                    String newKey = key.replace("FACET_", "");
                    if (globalBundle.containsKey("MD_" + newKey)) {
                        return globalBundle.getString("MD_" + newKey);
                    }
                    if (globalBundle.containsKey(newKey)) {
                        return globalBundle.getString(newKey);
                    }
                }
                return globalBundle.getString(key);
            } catch (RuntimeException e) {
                // This is needed for some reason
            }
        } else {
            logger.warn("globalBundle is null");
        }

        return key;
    }

    /**
     * <p>getMessagesValues.</p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @param keyPrefix a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> getMessagesValues(Locale locale, String keyPrefix) {
        ResourceBundle rb = loadLocalResourceBundle(locale);
        List<String> res = new ArrayList<>();

        for (String key : rb.keySet()) {
            if (key.startsWith(keyPrefix)) {
                res.add(key);
            }
        }

        Collections.sort(res);

        return res;
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration<String> getKeys() {
        return null;
    }

}

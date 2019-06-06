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

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractTest;
import io.goobi.viewer.connector.messages.MessageResourceBundle;

public class MessageResourceBundleTest extends AbstractTest {

    /**
     * @see MessageResourceBundle#getTranslation(String,Locale)
     * @verifies translate text correctly
     */
    @Test
    public void getTranslation_shouldTranslateTextCorrectly() throws Exception {
        Assert.assertEquals("Table", MessageResourceBundle.getTranslation("table", Locale.ENGLISH));
        Assert.assertEquals("Tabelle", MessageResourceBundle.getTranslation("table", Locale.GERMAN));
        Assert.assertEquals("Tabla", MessageResourceBundle.getTranslation("table", Locale.forLanguageTag("es")));
    }
}
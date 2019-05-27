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
package de.intranda.digiverso.m2m.utils;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.m2m.AbstractTest;

public class UtilsTest extends AbstractTest {

    /**
     * @see Utils#getDocumentFromString(String,String)
     * @verifies build document correctly
     */
    @Test
    public void getDocumentFromString_shouldBuildDocumentCorrectly() throws Exception {
        Assert.assertEquals("2016-05-23T10:40:00Z", Utils.convertDate(1464000000000L));
    }

    /**
     * @see Utils#splitIdentifierAndLanguageCode(String,int)
     * @verifies split identifier correctly
     */
    @Test
    public void splitIdentifierAndLanguageCode_shouldSplitIdentifierCorrectly() throws Exception {
        {
            String[] result = Utils.splitIdentifierAndLanguageCode("id_eng", 3);
            Assert.assertEquals(2, result.length);
            Assert.assertEquals("id", result[0]);
            Assert.assertEquals("eng", result[1]);
        }
        {
            String[] result = Utils.splitIdentifierAndLanguageCode("id", 3);
            Assert.assertEquals(2, result.length);
            Assert.assertEquals("id", result[0]);
            Assert.assertNull(result[1]);
        }

    }
}
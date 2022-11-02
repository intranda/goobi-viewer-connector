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

import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractTest;

public class UtilsTest extends AbstractTest {

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

    /**
     * @see Utils#convertDate(long)
     * @verifies convert time correctly
     */
    @Test
    public void convertDate_shouldConvertTimeCorrectly() throws Exception {
        Assert.assertEquals("2016-05-23T10:40:00Z", Utils.convertDate(1464000000000L));
    }
    
    /**
     * @see Utils#parseDate(Object)
     * @verifies parse dates correctly
     */
    @Test
    public void parseDate_shouldParseDatesCorrectly() throws Exception {
        Assert.assertEquals("2016-05-23T10:40:00Z", Utils.parseDate(1464000000000L));
    }

    /**
     * @see Utils#getCurrentUTCTime(long)
     * @verifies format time correctly
     */
    @Test
    public void getCurrentUTCTime_shouldFormatTimeCorrectly() throws Exception {
        Assert.assertEquals("2020-09-07T14:30:00Z", Utils.getCurrentUTCTime(LocalDateTime.of(2020, 9, 7, 14, 30, 00)));
    }
    

    /**
     * @see Utils#getCurrentUTCTime(LocalDateTime)
     * @verifies truncate to seconds
     */
    @Test
    public void getCurrentUTCTime_shouldTruncateToSeconds() throws Exception {
        Assert.assertEquals("2020-09-07T14:30:00Z", Utils.getCurrentUTCTime(LocalDateTime.of(2020, 9, 7, 14, 30, 00, 1000000)));
    }

    /**
     * @see Utils#cleanUpTimestamp(String)
     * @verifies clean up timestamp correctly
     */
    @Test
    public void cleanUpTimestamp_shouldCleanUpTimestampCorrectly() throws Exception {
        Assert.assertEquals("20201028133500", Utils.cleanUpTimestamp("2020-10-28T13:35:00"));
        Assert.assertEquals("20201028133500", Utils.cleanUpTimestamp("2020-10-28T13:35:00.000"));
    }
}
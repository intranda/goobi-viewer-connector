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
package io.goobi.viewer.connector.oai;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractTest;
import io.goobi.viewer.connector.oai.servlets.OaiServlet;

public class OaiServletTest extends AbstractTest {
    /**
     * @see OaiServlet#checkDatestamps(String,String)
     * @verifies return false if from is not well formed
     */
    @Test
    public void checkDatestamps_shouldReturnFalseIfFromIsNotWellFormed() throws Exception {
        Assert.assertFalse(OaiServlet.checkDatestamps("2015-09-30T15:00:00X", "2015-09-30T15:00:01"));
        Assert.assertFalse(OaiServlet.checkDatestamps("2015-09-2X", "2015-09-30"));
    }

    /**
     * @see OaiServlet#checkDatestamps(String,String)
     * @verifies return false if until is not well formed
     */
    @Test
    public void checkDatestamps_shouldReturnFalseIfUntilIsNotWellFormed() throws Exception {
        Assert.assertFalse(OaiServlet.checkDatestamps("2015-09-30T15:00:00", "2015-09-30T15:00:01Z"));
        Assert.assertFalse(OaiServlet.checkDatestamps("2015-09-30", "2015-09-31"));
    }

    /**
     * @see OaiServlet#checkDatestamps(String,String)
     * @verifies return false if from after until
     */
    @Test
    public void checkDatestamps_shouldReturnFalseIfFromAfterUntil() throws Exception {
        Assert.assertFalse(OaiServlet.checkDatestamps("2015-09-30T15:00:01", "2015-09-30T15:00:00"));
        Assert.assertFalse(OaiServlet.checkDatestamps("2015-09-30", "2015-09-29"));
    }

    /**
     * @see OaiServlet#checkDatestamps(String,String)
     * @verifies return true if from and until correct
     */
    @Test
    public void checkDatestamps_shouldReturnTrueIfFromAndUntilCorrect() throws Exception {
        Assert.assertTrue(OaiServlet.checkDatestamps("2015-09-30T15:00:00Z", "2015-09-30T15:00:01Z"));
        Assert.assertTrue(OaiServlet.checkDatestamps("2015-09-29", "2015-09-30"));
    }
}
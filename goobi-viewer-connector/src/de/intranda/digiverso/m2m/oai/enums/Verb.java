/**
 * This file is part of the Goobi Viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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

public enum Verb {

	GetRecord("GetRecord"), Identify("Identify"), ListIdentifiers("ListIdentifiers"), ListMetadataFormats("ListMetadataFormats"), ListRecords(
			"ListRecords"), ListSets("ListSets");

	private String title;

	private Verb(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public static Verb getByTitle(String title) {
		for (Verb v : Verb.values()) {
			if (v.getTitle().equals(title)) {
				return v;
			}
		}
		return null;
	}

}

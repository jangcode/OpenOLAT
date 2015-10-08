/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.course.assessment.model;

import org.olat.modules.assessment.ui.AssessmentToolSecurityCallback;
import org.olat.repository.RepositoryEntry;

/**
 * 
 * Initial date: 21.07.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class SearchAssessedIdentityParams {
	
	private final RepositoryEntry entry;
	private final RepositoryEntry referenceEntry;
	private final String subIdent;
	
	private final boolean admin;
	private final boolean nonMembers;
	private final boolean repositoryEntryCoach;
	private final boolean businessGroupCoach;
	
	
	public SearchAssessedIdentityParams(RepositoryEntry entry, RepositoryEntry referenceEntry, String subIdent,
			AssessmentToolSecurityCallback secCallback) {
		this.entry = entry;
		this.referenceEntry = referenceEntry;
		this.subIdent = subIdent;
		this.admin = secCallback.isAdmin();
		this.nonMembers = secCallback.canAssessNonMembers();
		this.repositoryEntryCoach = secCallback.canAssessRepositoryEntryMembers();
		this.businessGroupCoach = secCallback.canAssessBusinessGoupMembers();
	}
	
	public RepositoryEntry getEntry() {
		return entry;
	}
	
	public RepositoryEntry getReferenceEntry() {
		return referenceEntry;
	}

	public String getSubIdent() {
		return subIdent;
	}

	public boolean isAdmin() {
		return admin;
	}

	public boolean isNonMembers() {
		return nonMembers;
	}

	public boolean isRepositoryEntryCoach() {
		return repositoryEntryCoach;
	}

	public boolean isBusinessGroupCoach() {
		return businessGroupCoach;
	}
}

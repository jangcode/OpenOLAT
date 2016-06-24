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
package org.olat.modules.portfolio.ui;

import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiSortableColumnDef;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.modules.portfolio.ui.BinderAssessmentController.AssessmentSectionWrapper;

/**
 * 
 * Initial date: 22.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class BinderAssessmentDataModel extends DefaultFlexiTableDataModel<AssessmentSectionWrapper> {
	
	public BinderAssessmentDataModel(FlexiTableColumnModel columnsModel) {
		super(columnsModel);
	}
	
	@Override
	public Object getValueAt(int row, int col) {
		AssessmentSectionWrapper wrapper = getObject(row);
		switch(AssessmentSectionCols.values()[col]) {
			case sectionName: return wrapper.getSectionTitle();
			case numOfPages: return wrapper.getNumOfPages();
			case passed: {
				if(wrapper.getPassedEl() != null) {
					return wrapper.getPassedEl();
				}
				return wrapper.getPassed();
			}
			case score: {
				if(wrapper.getScoreEl() != null) {
					return wrapper.getScoreEl();
				}
				return wrapper.getScore();
			}
			case changeStatus: return wrapper.getButton();
		}
		return null;
	}
	
	@Override
	public DefaultFlexiTableDataModel<AssessmentSectionWrapper> createCopyWithEmptyList() {
		return new BinderAssessmentDataModel(getTableColumnModel());
	}

	public enum AssessmentSectionCols implements FlexiSortableColumnDef {
		sectionName("table.header.section"),
		numOfPages("table.header.numpages"),
		passed("table.header.passed"),
		score("table.header.score"),
		changeStatus("table.header.change.status");
		
		private final String i18nKey;
		
		private AssessmentSectionCols(String i18nKey) {
			this.i18nKey = i18nKey;
		}
		
		@Override
		public String i18nHeaderKey() {
			return i18nKey;
		}

		@Override
		public boolean sortable() {
			return true;
		}

		@Override
		public String sortKey() {
			return name();
		}
	}
}

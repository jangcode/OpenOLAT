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

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.StringHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.modules.portfolio.Binder;
import org.olat.modules.portfolio.BinderSecurityCallback;
import org.olat.modules.portfolio.BinderSecurityCallbackFactory;
import org.olat.modules.portfolio.PortfolioService;
import org.olat.modules.portfolio.model.MySharedItemRow;
import org.olat.modules.portfolio.ui.MySharedItemsDataModel.MySharedItemCols;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 15.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class MySharedItemsController extends FormBasicController implements Activateable2 {
	
	private FlexiTableElement tableEl;
	private MySharedItemsDataModel model;
	private final TooledStackedPanel stackPanel;

	private BinderController binderCtrl;
	
	@Autowired
	private PortfolioService portfolioService;
	
	public MySharedItemsController(UserRequest ureq, WindowControl wControl, TooledStackedPanel stackPanel) {
		super(ureq, wControl, "shared");
		this.stackPanel = stackPanel;
		
		initForm(ureq);
		loadModel();
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("my.shared.items");
		
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, MySharedItemCols.binderKey, "select"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(MySharedItemCols.binderName, "select"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(MySharedItemCols.courseName, "select"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(MySharedItemCols.lastModified));
	
		model = new MySharedItemsDataModel(columnsModel);
		tableEl = uifactory.addTableElement(getWindowControl(), "table", model, 20, false, getTranslator(), formLayout);
		tableEl.setSearchEnabled(true);
		tableEl.setCustomizeColumns(true);
		tableEl.setElementCssClass("o_binder_shared_items_listing");
		tableEl.setEmtpyTableMessageKey("table.sEmptyTable");
		tableEl.setPageSize(24);
		tableEl.setAndLoadPersistedPreferences(ureq, "my-shared-items");
	}
	
	private void loadModel() {
		List<Binder> portfolios = portfolioService.searchSharedBindersBy(getIdentity());
		List<MySharedItemRow> rows = new ArrayList<>(portfolios.size());
		for(Binder binder:portfolios) {
			MySharedItemRow row = new MySharedItemRow();
			row.setBinderKey(binder.getKey());
			row.setBinderTitle(binder.getTitle());
			row.setLastModified(binder.getLastModified());//TODO max()
			if(binder.getEntry() != null) {
				row.setCourseDisplayName(binder.getEntry().getDisplayname());
			}
			rows.add(row);
		}
		model.setObjects(rows);
		tableEl.reset();
		tableEl.reloadData();
	}
	
	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		//
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(tableEl == source) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				String cmd = se.getCommand();
				MySharedItemRow row = model.getObject(se.getIndex());
				if("select".equals(cmd)) {
					Activateable2 activateable = doSelectBinder(ureq, row);
					if(activateable != null) {
						activateable.activate(ureq, null, null);
					}
				}
			}
			
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	private BinderController doSelectBinder(UserRequest ureq, MySharedItemRow row) {
		Binder binder = portfolioService.getBinderByKey(row.getBinderKey());
		if(binder == null) {
			showWarning("warning.portfolio.not.found");
			return null;
		} else {
			removeAsListenerAndDispose(binderCtrl);
			
			OLATResourceable binderOres = OresHelper.createOLATResourceableInstance("Binder", binder.getKey());
			WindowControl swControl = addToHistory(ureq, binderOres, null);
			BinderSecurityCallback secCallback = BinderSecurityCallbackFactory.getCallbackForOwnedBinder(binder);
			binderCtrl = new BinderController(ureq, swControl, stackPanel, secCallback, binder);
			String displayName = StringHelper.escapeHtml(binder.getTitle());
			stackPanel.pushController(displayName, binderCtrl);
			return binderCtrl;
		}
	}
}

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
package org.olat.ims.qti21.pool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.i18n.I18nModule;
import org.olat.core.util.vfs.LocalImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.fileresource.types.ImsQTI21Resource;
import org.olat.ims.qti.QTIConstants;
import org.olat.ims.qti.editor.QTIEditHelper;
import org.olat.ims.qti.editor.QTIEditorPackage;
import org.olat.ims.qti.editor.beecom.objects.Item;
import org.olat.ims.qti21.QTI21Constants;
import org.olat.ims.qti21.QTI21Service;
import org.olat.ims.qti21.model.QTI21QuestionType;
import org.olat.ims.qti21.model.xml.AssessmentItemBuilder;
import org.olat.ims.qti21.model.xml.AssessmentItemMetadata;
import org.olat.ims.qti21.model.xml.ManifestBuilder;
import org.olat.ims.qti21.model.xml.ManifestMetadataBuilder;
import org.olat.ims.qti21.model.xml.interactions.EssayAssessmentItemBuilder;
import org.olat.ims.qti21.model.xml.interactions.FIBAssessmentItemBuilder;
import org.olat.ims.qti21.model.xml.interactions.FIBAssessmentItemBuilder.EntryType;
import org.olat.ims.qti21.model.xml.interactions.HotspotAssessmentItemBuilder;
import org.olat.ims.qti21.model.xml.interactions.KPrimAssessmentItemBuilder;
import org.olat.ims.qti21.model.xml.interactions.MultipleChoiceAssessmentItemBuilder;
import org.olat.ims.qti21.model.xml.interactions.SingleChoiceAssessmentItemBuilder;
import org.olat.ims.resources.IMSEntityResolver;
import org.olat.imscp.xml.manifest.ResourceType;
import org.olat.modules.qpool.ExportFormatOptions;
import org.olat.modules.qpool.ExportFormatOptions.Outcome;
import org.olat.modules.qpool.QItemFactory;
import org.olat.modules.qpool.QPoolSPI;
import org.olat.modules.qpool.QPoolService;
import org.olat.modules.qpool.QuestionItem;
import org.olat.modules.qpool.QuestionItemFull;
import org.olat.modules.qpool.QuestionItemShort;
import org.olat.modules.qpool.manager.QEducationalContextDAO;
import org.olat.modules.qpool.manager.QItemTypeDAO;
import org.olat.modules.qpool.manager.QLicenseDAO;
import org.olat.modules.qpool.manager.QPoolFileStorage;
import org.olat.modules.qpool.manager.QuestionItemDAO;
import org.olat.modules.qpool.manager.TaxonomyLevelDAO;
import org.olat.modules.qpool.model.DefaultExportFormat;
import org.olat.modules.qpool.model.QuestionItemImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.test.AssessmentItemRef;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentItem;

/**
 * 
 * Initial date: 05.02.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service("qti21PoolServiceProvider")
public class QTI21QPoolServiceProvider implements QPoolSPI {
	
	private static final OLog log = Tracing.createLoggerFor(QTI21QPoolServiceProvider.class);
	
	public static final String QTI_12_OO_TEST = "OpenOLAT Test";

	@Autowired
	private QTI21Service qtiService;

	@Autowired
	private QPoolService qpoolService;
	@Autowired
	private QPoolFileStorage qpoolFileStorage;
	@Autowired
	private QLicenseDAO qLicenseDao;
	@Autowired
	private QItemTypeDAO qItemTypeDao;
	@Autowired
	private QuestionItemDAO questionItemDao;
	@Autowired
	private QEducationalContextDAO qEduContextDao;
	@Autowired
	private TaxonomyLevelDAO taxonomyLevelDao;
	
	private static final List<ExportFormatOptions> formats = new ArrayList<ExportFormatOptions>(4);
	static {
		formats.add(DefaultExportFormat.ZIP_EXPORT_FORMAT);
		formats.add(DefaultExportFormat.DOCX_EXPORT_FORMAT);
		formats.add(new DefaultExportFormat(QTI21Constants.QTI_21_FORMAT, Outcome.download, null));
		formats.add(new DefaultExportFormat(QTI21Constants.QTI_21_FORMAT, Outcome.repository, ImsQTI21Resource.TYPE_NAME));
	}
	
	
	public QTI21QPoolServiceProvider() {
		//
	}

	@Override
	public int getPriority() {
		return 10;
	}

	@Override
	public String getFormat() {
		return QTI21Constants.QTI_21_FORMAT;
	}

	@Override
	public List<ExportFormatOptions> getTestExportFormats() {
		return Collections.unmodifiableList(formats);
	}

	@Override
	public boolean isCompatible(String filename, File file) {
		boolean ok = new AssessmentItemFileResourceValidator().validate(filename, file);
		return ok;
	}
	@Override
	public boolean isCompatible(String filename, VFSLeaf file) {
		boolean ok = new AssessmentItemFileResourceValidator().validate(filename, file);
		return ok;
	}
	
	@Override
	public boolean isConversionPossible(QuestionItemShort item) {
		if(QTIConstants.QTI_12_FORMAT.equals(item.getFormat())) {
			VFSLeaf leaf = qpoolService.getRootLeaf(item);
			if(leaf == null) {
				return false;
			} else {
				Item qtiItem = QTIEditHelper.readItemXml(leaf);
				return qtiItem != null && !qtiItem.isAlient();
			}
		}
		return false;
	}

	@Override
	public List<QItemFactory> getItemfactories() {
		List<QItemFactory> factories = new ArrayList<QItemFactory>();
		for(QTI21QuestionType type:QTI21QuestionType.values()) {
			if(type.hasEditor()) {
				factories.add(new QTI21AssessmentItemFactory(type));
			}
		}
		return factories;
	}

	@Override
	public String extractTextContent(QuestionItemFull item) {
		String content = null;
		if(item.getRootFilename() != null) {
			String dir = item.getDirectory();
			VFSContainer container = qpoolFileStorage.getContainer(dir);
			VFSItem file = container.resolve(item.getRootFilename());
			if(file instanceof VFSLeaf) {
				VFSLeaf leaf = (VFSLeaf)file;
				QTI21SAXHandler handler = new QTI21SAXHandler();
				try(InputStream is = leaf.getInputStream()) {
					XMLReader parser = XMLReaderFactory.createXMLReader();
					parser.setContentHandler(handler);
					parser.setEntityResolver(new IMSEntityResolver());
					parser.setFeature("http://xml.org/sax/features/validation", false);
					parser.parse(new InputSource(is));
				} catch (Exception e) {
					log.error("", e);
				}
				return handler.toString();
			}
		}
		return content;
	}

	@Override
	public List<QuestionItem> importItems(Identity owner, Locale defaultLocale, String filename, File file) {
		QTI21ImportProcessor processor = new QTI21ImportProcessor(owner, defaultLocale,
				questionItemDao, qItemTypeDao, qEduContextDao, taxonomyLevelDao, qLicenseDao, qpoolFileStorage, qtiService);
		return processor.process(file);
	}

	@Override
	public MediaResource exportTest(List<QuestionItemShort> items, ExportFormatOptions format, Locale locale) {
		if(QTI21Constants.QTI_21_FORMAT.equals(format.getFormat())) {
			return new QTI21ExportTestResource("UTF-8", locale, items, this);
		} else if(DefaultExportFormat.DOCX_EXPORT_FORMAT.getFormat().equals(format.getFormat())) {
			return new QTI12And21PoolWordExport(items, I18nModule.getDefaultLocale(), "UTF-8", questionItemDao, qpoolFileStorage);
		}
		return null;
	}

	@Override
	public void exportItem(QuestionItemFull item, ZipOutputStream zout, Locale locale, Set<String> names) {
		QTI21ExportProcessor processor = new QTI21ExportProcessor(qtiService, qpoolFileStorage, locale);
		processor.process(item, zout, names);
	}

	@Override
	public void copyItem(QuestionItemFull original, QuestionItemFull copy) {
		VFSContainer originalDir = qpoolFileStorage.getContainer(original.getDirectory());
		VFSContainer copyDir = qpoolFileStorage.getContainer(copy.getDirectory());
		VFSManager.copyContent(originalDir, copyDir);
	}

	@Override
	public QuestionItem convert(Identity owner, QuestionItemShort itemToConvert, Locale locale) {
		if(QTIConstants.QTI_12_FORMAT.equals(itemToConvert.getFormat())) {
			VFSLeaf leaf = qpoolService.getRootLeaf(itemToConvert);
			if(leaf == null) {
				return null;
			} else {
				Item qtiItem = QTIEditHelper.readItemXml(leaf);
				if(qtiItem != null && !qtiItem.isAlient()) {
					QuestionItemImpl original = questionItemDao.loadById(itemToConvert.getKey());
					QuestionItemImpl copy = questionItemDao.copy(original);
					copy.setTitle(original.getTitle());
					copy.setFormat(getFormat());
					
					VFSContainer originalDir = qpoolFileStorage.getContainer(original.getDirectory());
					File copyDir = qpoolFileStorage.getDirectory(copy.getDirectory());

					QTI12To21Converter converter = new QTI12To21Converter(copyDir, locale);
					if(converter.convert(copy, qtiItem, originalDir)) {
						questionItemDao.persist(owner, copy);
						return copy;
					}
				}
			}
		}

		return null;
	}

	@Override
	public Controller getPreviewController(UserRequest ureq, WindowControl wControl, QuestionItem item, boolean summary) {
		return new QTI21PreviewController(ureq, wControl, item);
	}

	@Override
	public boolean isTypeEditable() {
		return true;
	}

	@Override
	public Controller getEditableController(UserRequest ureq, WindowControl wControl, QuestionItem qitem) {
		Controller editorCtrl = new QTI21EditorController(ureq, wControl, qitem);
		return editorCtrl;
	}

	public QuestionItem createItem(Identity identity, QTI21QuestionType type, String title, Locale locale) {
		AssessmentItemBuilder itemBuilder = null;
		switch(type) {
			case sc: itemBuilder = new SingleChoiceAssessmentItemBuilder(qtiService.qtiSerializer()); break;
			case mc: itemBuilder = new MultipleChoiceAssessmentItemBuilder(qtiService.qtiSerializer()); break;
			case kprim: itemBuilder = new KPrimAssessmentItemBuilder(qtiService.qtiSerializer()); break;
			case fib: itemBuilder = new FIBAssessmentItemBuilder(EntryType.text, qtiService.qtiSerializer()); break;
			case numerical: itemBuilder = new FIBAssessmentItemBuilder(EntryType.numerical, qtiService.qtiSerializer()); break;
			case essay: itemBuilder = new EssayAssessmentItemBuilder(qtiService.qtiSerializer()); break;
			case hotspot: itemBuilder = new HotspotAssessmentItemBuilder(qtiService.qtiSerializer()); break;
			default: return null;
		}

		AssessmentItem assessmentItem = itemBuilder.getAssessmentItem();
		assessmentItem.setLabel(title);
		assessmentItem.setTitle(title);
		
		AssessmentItemMetadata itemMetadata = new AssessmentItemMetadata();
		itemMetadata.setQuestionType(type);
		
		QTI21ImportProcessor processor = new QTI21ImportProcessor(identity, locale, 
				questionItemDao, qItemTypeDao, qEduContextDao, taxonomyLevelDao, qLicenseDao, qpoolFileStorage, qtiService);
		QuestionItemImpl qitem = processor.processItem(assessmentItem, "", null, "OpenOLAT", Settings.getVersion(), itemMetadata);

		VFSContainer baseDir = qpoolFileStorage.getContainer(qitem.getDirectory());
		VFSLeaf leaf = baseDir.createChildLeaf(qitem.getRootFilename());
		File itemFile = ((LocalImpl)leaf).getBasefile();
		qtiService.persistAssessmentObject(itemFile, assessmentItem);
		
		//create imsmanifest
		ManifestBuilder manifest = ManifestBuilder.createAssessmentItemBuilder();
		manifest.appendAssessmentItem(itemFile.getName());	
		manifest.write(new File(itemFile.getParentFile(), "imsmanifest.xml"));
		return qitem;
	}
	
	/**
	 * Very important, the ManifestMetadataBuilder will be changed, it need to be a clone
	 * 
	 * 
	 * @param owner
	 * @param itemRef
	 * @param assessmentItem
	 * @param clonedMetadataBuilder
	 * @param fUnzippedDirRoot
	 * @param defaultLocale
	 */
	public void importAssessmentItemRef(Identity owner,  AssessmentItemRef itemRef, AssessmentItem assessmentItem,
			ManifestMetadataBuilder clonedMetadataBuilder, File fUnzippedDirRoot, Locale defaultLocale) {
		QTI21ImportProcessor processor =  new QTI21ImportProcessor(owner, defaultLocale,
				questionItemDao, qItemTypeDao, qEduContextDao, taxonomyLevelDao, qLicenseDao, qpoolFileStorage, qtiService);
		
		AssessmentItemMetadata metadata = new AssessmentItemMetadata(clonedMetadataBuilder);

		String editor = null;
		String editorVersion = null;
		if(StringHelper.containsNonWhitespace(assessmentItem.getToolName())) {
			editor = assessmentItem.getToolName();
		}
		if(StringHelper.containsNonWhitespace(assessmentItem.getToolVersion())) {
			editorVersion = assessmentItem.getToolVersion();
		}

		File itemFile = new File(fUnzippedDirRoot, itemRef.getHref().toString());
		String originalItemFilename = itemFile.getName();

		QuestionItemImpl qitem = processor.processItem(assessmentItem, null, originalItemFilename,
				editor, editorVersion, metadata);
		
		//storage
		File itemStorage = qpoolFileStorage.getDirectory(qitem.getDirectory());
		FileUtils.copyDirContentsToDir(itemFile, itemStorage, false, "QTI21 import item xml in pool");
		
		//create manifest
		ManifestBuilder manifest = ManifestBuilder.createAssessmentItemBuilder();
		ResourceType resource = manifest.appendAssessmentItem(UUID.randomUUID().toString(), originalItemFilename);
		ManifestMetadataBuilder exportedMetadataBuilder = manifest.getMetadataBuilder(resource, true);
		exportedMetadataBuilder.setMetadata(clonedMetadataBuilder.getMetadata());
		manifest.write(new File(itemStorage, "imsmanifest.xml"));
		
		//process material
		List<String> materials = processor.getMaterials(assessmentItem);
		for(String material:materials) {
			if(material.indexOf("://") < 0) {// material can be an external URL
				File materialFile = new File(fUnzippedDirRoot, material);
				if(materialFile.isFile() && materialFile.exists()) {
					FileUtils.copyDirContentsToDir(materialFile, itemStorage, false, "QTI21 import material in pool");
				}
			}
		}
	}
	
	/**
	 * Export to QTI editor an item from the pool. The ident of the item
	 * is always regenerated as an UUID.
	 * @param qitem
	 * @param editorContainer
	 * @return
	 */
	public AssessmentItem exportToQTIEditor(QuestionItemShort qitem, Locale locale, File editorContainer) throws IOException {
		QTI21ExportProcessor processor = new QTI21ExportProcessor(qtiService, qpoolFileStorage, locale);
		QuestionItemFull fullItem = questionItemDao.loadById(qitem.getKey());
		ResolvedAssessmentItem resolvedAssessmentItem = processor.exportToQTIEditor(fullItem, editorContainer);
		AssessmentItem assessmentItem = resolvedAssessmentItem.getItemLookup().extractAssumingSuccessful();
		assessmentItem.setIdentifier(QTI21QuestionType.generateNewIdentifier(assessmentItem.getIdentifier()));
		return assessmentItem;
	}
	
	public void assembleTest(List<QuestionItemShort> items, Locale locale, ZipOutputStream zout) {
		List<Long> itemKeys = new ArrayList<Long>();
		for(QuestionItemShort item:items) {
			itemKeys.add(item.getKey());
		}

		List<QuestionItemFull> fullItems = questionItemDao.loadByIds(itemKeys);
		QTI21ExportProcessor processor = new QTI21ExportProcessor(qtiService, qpoolFileStorage, locale);
		processor.assembleTest(fullItems, zout);	
	}
	
	public void exportToEditorPackage(File exportDir, List<QuestionItemShort> items, Locale locale) {
		List<Long> itemKeys = toKeys(items);
		List<QuestionItemFull> fullItems = questionItemDao.loadByIds(itemKeys);

		QTI21ExportProcessor processor = new QTI21ExportProcessor(qtiService, qpoolFileStorage, locale);
		processor.assembleTest(fullItems, exportDir);
	}
	
	/**
	 * Convert from QTI 1.2 to 2.1
	 * 
	 * @param qtiEditorPackage
	 */
	public boolean convertFromEditorPackage(QTIEditorPackage qtiEditorPackage, File unzippedDirRoot, Locale locale) {
		try {
			QTI12To21Converter converter = new QTI12To21Converter(unzippedDirRoot, locale);
			converter.convert(qtiEditorPackage);
			return true;
		} catch (URISyntaxException e) {
			log.error("", e);
			return false;
		}
	}
	
	private List<Long> toKeys(List<? extends QuestionItemShort> items) {
		List<Long> keys = new ArrayList<Long>(items.size());
		for(QuestionItemShort item:items) {
			keys.add(item.getKey());
		}
		return keys;
	}
	

}
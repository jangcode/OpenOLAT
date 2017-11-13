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
package org.olat.modules.taxonomy.manager;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.persistence.DB;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.modules.taxonomy.Taxonomy;
import org.olat.modules.taxonomy.TaxonomyLevel;
import org.olat.modules.taxonomy.TaxonomyLevelManagedFlag;
import org.olat.modules.taxonomy.TaxonomyLevelRef;
import org.olat.modules.taxonomy.TaxonomyLevelType;
import org.olat.modules.taxonomy.TaxonomyRef;
import org.olat.modules.taxonomy.TaxonomyService;
import org.olat.modules.taxonomy.model.TaxonomyLevelImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * 
 * Initial date: 22 sept. 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class TaxonomyLevelDAO implements InitializingBean {

	private File rootDirectory, taxonomyLevelDirectory;
	
	@Autowired
	private DB dbInstance;
	
	@Override
	public void afterPropertiesSet() {
		File bcrootDirectory = new File(FolderConfig.getCanonicalRoot());
		rootDirectory = new File(bcrootDirectory, "taxonomy");
		taxonomyLevelDirectory = new File(rootDirectory, "levels");
		if(!taxonomyLevelDirectory.exists()) {
			taxonomyLevelDirectory.mkdirs();
		}
	}
	
	public TaxonomyLevel createTaxonomyLevel(String identifier, String displayName, String description,
			String externalId, TaxonomyLevelManagedFlag[] flags,
			TaxonomyLevel parent, TaxonomyLevelType type, Taxonomy taxonomy) {
		TaxonomyLevelImpl level = new TaxonomyLevelImpl();
		level.setCreationDate(new Date());
		level.setLastModified(level.getCreationDate());
		level.setEnabled(true);
		if(StringHelper.containsNonWhitespace(identifier)) {
			level.setIdentifier(identifier);
		} else {
			level.setIdentifier(UUID.randomUUID().toString());
		}
		level.setManagedFlagsString(TaxonomyLevelManagedFlag.toString(flags));
		level.setDisplayName(displayName);
		level.setDescription(description);
		level.setExternalId(externalId);
		level.setTaxonomy(taxonomy);
		level.setType(type);
		
		dbInstance.getCurrentEntityManager().persist(level);
		String storage = createLevelStorage(taxonomy, level);
		level.setDirectoryPath(storage);
		
		String identifiersPath = getMaterializedPathIdentifiers(parent, level);
		String keysPath = getMaterializedPathKeys(parent, level);
		level.setParent(parent);
		level.setMaterializedPathKeys(keysPath);
		level.setMaterializedPathIdentifiers(identifiersPath);

		level = dbInstance.getCurrentEntityManager().merge(level);
		level.getTaxonomy();
		return level;
	}
	
	private String getMaterializedPathIdentifiers(TaxonomyLevel parent, TaxonomyLevel level) {
		if(parent != null) {
			String parentPathOfIdentifiers = parent.getMaterializedPathIdentifiers();
			if(parentPathOfIdentifiers == null || "/".equals(parentPathOfIdentifiers)) {
				parentPathOfIdentifiers = "/";
			}
			return parentPathOfIdentifiers + level.getIdentifier()  + "/";
		}
		return "/" + level.getIdentifier()  + "/";
	}
	
	private String getMaterializedPathKeys(TaxonomyLevel parent, TaxonomyLevel level) {
		if(parent != null) {

			String parentPathOfKeys = parent.getMaterializedPathKeys();
			if(parentPathOfKeys == null || "/".equals(parentPathOfKeys)) {
				parentPathOfKeys = "";
			}

			return parentPathOfKeys + level.getKey() + "/";
		}
		return "/" + level.getKey() + "/";
	}
	
	public TaxonomyLevel loadByKey(Long key) {
		List<TaxonomyLevel> levels = dbInstance.getCurrentEntityManager()
				.createNamedQuery("loadTaxonomyLevelsByKey", TaxonomyLevel.class)
				.setParameter("levelKey", key)
				.getResultList();
		return levels == null || levels.isEmpty() ? null : levels.get(0);
	}
	
	public List<TaxonomyLevel> getLevels(TaxonomyRef taxonomy) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select level from ctaxonomylevel as level")
		  .append(" left join fetch level.parent as parent")
		  .append(" left join fetch level.type as type")
		  .append(" inner join fetch level.taxonomy as taxonomy");
		if(taxonomy != null) {
			sb.append(" where level.taxonomy.key=:taxonomyKey");
		}
		
		TypedQuery<TaxonomyLevel> query = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), TaxonomyLevel.class);
		if(taxonomy != null) {
			query.setParameter("taxonomyKey", taxonomy.getKey());
		}
		return query.getResultList();
	}
	
	public List<TaxonomyLevel> getLevelsByExternalId(TaxonomyRef taxonomy, String externalId) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select level from ctaxonomylevel as level")
		  .append(" left join fetch level.parent as parent")
		  .append(" left join fetch level.type as type")
		  .append(" inner join fetch level.taxonomy as taxonomy")
		  .append(" where level.taxonomy.key=:taxonomyKey and level.externalId=:externalId");

		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), TaxonomyLevel.class)
			.setParameter("taxonomyKey", taxonomy.getKey())
			.setParameter("externalId", externalId)
			.getResultList();
	}
	
	public List<TaxonomyLevel> getLevelsByDisplayName(TaxonomyRef taxonomy, String displayName) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select level from ctaxonomylevel as level")
		  .append(" left join fetch level.parent as parent")
		  .append(" left join fetch level.type as type")
		  .append(" inner join fetch level.taxonomy as taxonomy")
		  .append(" where level.taxonomy.key=:taxonomyKey and level.displayName=:displayName");
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), TaxonomyLevel.class)
			.setParameter("taxonomyKey", taxonomy.getKey())
			.setParameter("displayName", displayName)
			.getResultList();
	}
	
	public TaxonomyLevel getParent(TaxonomyLevelRef taxonomyLevel) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select level.parent from ctaxonomylevel as level")
		  .append(" where level.key=:taxonomyLevelKey");
		List<TaxonomyLevel> levels = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), TaxonomyLevel.class)
				.setParameter("taxonomyLevelKey", taxonomyLevel.getKey())
				.getResultList();
		return levels == null || levels.isEmpty() ? null : levels.get(0);
	}
	
	// Perhaps replace it with a select in ( materializedPathKeys.split("[/]") ) would be better
	public List<TaxonomyLevel> getParentLine(TaxonomyLevel taxonomyLevel, TaxonomyRef taxonomy) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select level from ctaxonomylevel as level")
		  .append(" left join fetch level.parent as parent")
		  .append(" left join fetch level.type as type")
		  .append(" where level.taxonomy.key=:taxonomyKey")
		  .append(" and locate(level.materializedPathKeys,:materializedPath) = 1");
		  
		List<TaxonomyLevel> levels = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), TaxonomyLevel.class)
			.setParameter("materializedPath", taxonomyLevel.getMaterializedPathKeys() + "%")
			.setParameter("taxonomyKey", taxonomy.getKey())
			.getResultList();
		Collections.sort(levels, new PathMaterializedPathLengthComparator());
		return levels;
	}
	
	public List<TaxonomyLevel> getDescendants(TaxonomyLevel taxonomyLevel, TaxonomyRef taxonomy) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("select level from ctaxonomylevel as level")
		  .append(" left join fetch level.parent as parent")
		  .append(" left join fetch level.type as type")
		  .append(" where level.taxonomy.key=:taxonomyKey")
		  .append(" and level.key!=:levelKey and level.materializedPathKeys like :materializedPath");
		  
		List<TaxonomyLevel> levels = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), TaxonomyLevel.class)
			.setParameter("materializedPath", taxonomyLevel.getMaterializedPathKeys() + "%")
			.setParameter("levelKey", taxonomyLevel.getKey())
			.setParameter("taxonomyKey", taxonomy.getKey())
			.getResultList();
		Collections.sort(levels, new PathMaterializedPathLengthComparator());
		return levels;
	}
	
	public TaxonomyLevel updateTaxonomyLevel(TaxonomyLevel level) {
		boolean updatePath = false;
		
		String path = level.getMaterializedPathIdentifiers();
		String newPath = null;
		
		TaxonomyLevel parentLevel = getParent(level);
		if(parentLevel != null) {
			newPath = getMaterializedPathIdentifiers(parentLevel, level);
			updatePath = !newPath.equals(path);
			if(updatePath) {
				((TaxonomyLevelImpl)level).setMaterializedPathIdentifiers(newPath);
			}
		}

		((TaxonomyLevelImpl)level).setLastModified(new Date());
		TaxonomyLevel mergedLevel = dbInstance.getCurrentEntityManager().merge(level);
		
		if(updatePath) {
			List<TaxonomyLevel> descendants = getDescendants(mergedLevel, mergedLevel.getTaxonomy());
			for(TaxonomyLevel descendant:descendants) {
				String descendantPath = descendant.getMaterializedPathIdentifiers();
				if(descendantPath.indexOf(path) == 0) {
					String end = descendantPath.substring(path.length(), descendantPath.length());
					String updatedPath = newPath + end;
					((TaxonomyLevelImpl)descendant).setMaterializedPathIdentifiers(updatedPath);
				}
				dbInstance.getCurrentEntityManager().merge(descendant);
			}
		}
		return mergedLevel;
	}
	
	/**
	 * Move use a lock on the taxonomy to prevent concurrent changes. Therefore
	 * the method will commit the changes as soon as possible.
	 * 
	 * @param level The level to move
	 * @param newParentLevel The new parent level, if null the level move as a root
	 * @return The updated level
	 */
	public TaxonomyLevel moveTaxonomyLevel(TaxonomyLevel level, TaxonomyLevel newParentLevel) {
		@SuppressWarnings("unused")
		Taxonomy lockedTaxonomy = loadForUpdate(level.getTaxonomy());

		TaxonomyLevel parentLevel = getParent(level);
		if(parentLevel == null && newParentLevel == null) {
			return level;//already root
		} else if(parentLevel != null && parentLevel.equals(newParentLevel)) {
			return level;//same parent
		}

		String keysPath = level.getMaterializedPathKeys();
		String identifiersPath = level.getMaterializedPathIdentifiers();
		
		List<TaxonomyLevel> descendants = getDescendants(level, level.getTaxonomy());
		TaxonomyLevelImpl levelImpl = (TaxonomyLevelImpl)level;
		levelImpl.setParent(newParentLevel);
		levelImpl.setLastModified(new Date());
		String newKeysPath = getMaterializedPathKeys(newParentLevel, levelImpl);
		String newIdentifiersPath = getMaterializedPathIdentifiers(newParentLevel, levelImpl);
		levelImpl.setMaterializedPathKeys(newKeysPath);
		levelImpl.setMaterializedPathIdentifiers(newIdentifiersPath);
		levelImpl = dbInstance.getCurrentEntityManager().merge(levelImpl);

		for(TaxonomyLevel descendant:descendants) {
			String descendantKeysPath = descendant.getMaterializedPathIdentifiers();
			String descendantIdentifiersPath = descendant.getMaterializedPathIdentifiers();
			if(descendantIdentifiersPath.indexOf(identifiersPath) == 0) {
				String end = descendantIdentifiersPath.substring(identifiersPath.length(), descendantIdentifiersPath.length());
				String updatedPath = newIdentifiersPath + end;
				((TaxonomyLevelImpl)descendant).setMaterializedPathIdentifiers(updatedPath);
			}
			if(descendantKeysPath.indexOf(keysPath) == 0) {
				String end = descendantKeysPath.substring(keysPath.length(), descendantKeysPath.length());
				String updatedPath = newKeysPath + end;
				((TaxonomyLevelImpl)descendant).setMaterializedPathIdentifiers(updatedPath);
			}
			dbInstance.getCurrentEntityManager().merge(descendant);
		}
		
		dbInstance.commit();
		return levelImpl;
	}
	
	public boolean delete(TaxonomyLevelRef taxonomyLevel) {
		if(!hasChildren(taxonomyLevel) && !hasItemUsing(taxonomyLevel) &&!hasCompetenceUsing(taxonomyLevel)) {
			TaxonomyLevel impl = loadByKey(taxonomyLevel.getKey());
			if(impl != null) {
				dbInstance.getCurrentEntityManager().remove(impl);
			}
			return true;
		}
		return false;
	}
	
	public boolean hasItemUsing(TaxonomyLevelRef taxonomyLevel) {
		String sb = "select item.key from questionitem item where item.taxonomyLevel.key=:taxonomyLevelKey";
		List<Long> items = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Long.class)
				.setParameter("taxonomyLevelKey", taxonomyLevel.getKey())
				.setFirstResult(0)
				.setMaxResults(1)
				.getResultList();
		return items != null && items.size() > 0 && items.get(0) != null && items.get(0).intValue() > 0;
	}
	
	public boolean hasCompetenceUsing(TaxonomyLevelRef taxonomyLevel) {
		String sb = "select competence.key from ctaxonomycompetence competence where competence.taxonomyLevel.key=:taxonomyLevelKey";
		List<Long> comptences = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Long.class)
				.setParameter("taxonomyLevelKey", taxonomyLevel.getKey())
				.setFirstResult(0)
				.setMaxResults(1)
				.getResultList();
		return comptences != null && comptences.size() > 0 && comptences.get(0) != null && comptences.get(0).intValue() > 0;
	}
	
	public boolean hasChildren(TaxonomyLevelRef taxonomyLevel) {
		String sb = "select level.key from ctaxonomylevel as level where level.parent.key=:taxonomyLevelKey";
		List<Long> children = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Long.class)
			.setParameter("taxonomyLevelKey", taxonomyLevel.getKey())
			.setFirstResult(0)
			.setMaxResults(1)
			.getResultList();
		return children != null && children.size() > 0 && children.get(0) != null && children.get(0).intValue() > 0;
	}
	
	public Taxonomy loadForUpdate(Taxonomy taxonomy) {
		//first remove it from caches
		dbInstance.getCurrentEntityManager().detach(taxonomy);

		String query = "select taxonomy from ctaxonomy taxonomy where taxonomy.key=:taxonomyKey";
		List<Taxonomy> entries = dbInstance.getCurrentEntityManager()
				.createQuery(query.toString(), Taxonomy.class)
				.setParameter("taxonomyKey", taxonomy.getKey())
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.getResultList();
		return entries == null || entries.isEmpty() ? null : entries.get(0);
	}
	
	public VFSContainer getDocumentsLibrary(TaxonomyLevel level) {
		String path = ((TaxonomyLevelImpl)level).getDirectoryPath();
		if(!path.startsWith("/")) {
			path = "/" + path;
		}
		path = "/" + TaxonomyService.DIRECTORY + path;
		return new OlatRootFolderImpl(path, null);
	}
	
	public String createLevelStorage(Taxonomy taxonomy, TaxonomyLevel level) {
		File taxonomyDirectory = new File(taxonomyLevelDirectory, taxonomy.getKey().toString());
		File storage = new File(taxonomyDirectory, level.getKey().toString());
		storage.mkdirs();
		
		Path relativePath = rootDirectory.toPath().relativize(storage.toPath());
		String relativePathString = relativePath.toString();
		return relativePathString;
	}
	
	private static class PathMaterializedPathLengthComparator implements Comparator<TaxonomyLevel> {
		@Override
		public int compare(TaxonomyLevel l1, TaxonomyLevel l2) {
			String s1 = l1.getMaterializedPathKeys();
			String s2 = l2.getMaterializedPathKeys();
			
			int len1 = s1 == null ? 0 : s1.length();
			int len2 = s2 == null ? 0 : s2.length();
			return len1 - len2;
		}
	}
}

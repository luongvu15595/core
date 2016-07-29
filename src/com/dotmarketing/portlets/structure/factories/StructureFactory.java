package com.dotmarketing.portlets.structure.factories;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ToStructureTransformer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cmis.QueryResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.PermissionedWebAssetUtil;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkFlowFactory;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Provides access to information related to Content Types and the different
 * ways it is related to other types of objects in dotCMS. The term "Structure" 
 * is deprecated, it has been changed to "Content Type" now. 
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
@Deprecated
public class StructureFactory {

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

	private static HostAPI hostAPI = APILocator.getHostAPI();


	/**
	 * @param permissionAPI the permissionAPI to set
	 */
	public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

	//### READ ###

	/**
	 * Gets the structure by inode
	 * @deprecated  Use CacheLocator.getContentTypeCache().getStructureByInode instead
	 * @param inode is the contentlet inode
	 */
	public static Structure getStructureByInode(String inode) {
		try {
			return new ToStructureTransformer(APILocator.getContentTypeAPI2().find(inode,APILocator.systemUser())).from();
		} catch (Exception e) {
			throw new DotStateException(e);
		}
	}
	/**
	 * Gets the structure by Type
	 * @deprecated  Use CacheLocator.getContentTypeCache().getStructureByName instead
	 * @param type is the name of the structure
	 */
	public static Structure getStructureByType(String type)
	{
		type = SQLUtil.sanitizeParameter(type);
		String condition = " name = '" + type + "'";
		try {
			return new ToStructureTransformer(APILocator.getContentTypeAPI2().find(condition, "mod_date", 1, 0, "desc", APILocator.systemUser(), false)).from();
		} catch (DotStateException | DotDataException e) {
			throw new DotStateException(e);
		}
	}

	/**
	 * Gets the structure by variable name
	 * @param type is the name of the structure
	 */
	@SuppressWarnings("unchecked")
	public static Structure getStructureByVelocityVarName(String varName)
	{

		try {
			return new ToStructureTransformer(APILocator.getContentTypeAPI2().findByVarName(varName,APILocator.systemUser())).from();
		} catch (Exception e) {
			throw new DotStateException(e);
		}
	}

	public static Structure getDefaultStructure()
	{
		try {
			return new ToStructureTransformer(APILocator.getContentTypeAPI2().findDefault(APILocator.systemUser())).from();
		} catch (Exception e) {
			throw new DotStateException(e);
		}
	}

	/**
	 * This method return the structures s
	 * @return List<String>
	 */
	@Deprecated
	public static List<String> getAllStructuresNames()
	{
	
			return new ArrayList<String>();
	}
	
	@Deprecated
	public static List<String> getAllVelocityVariablesNames()
	{
		return new ArrayList<String>();

	}
	
	@Deprecated
	public static List<SimpleStructureURLMap> findStructureURLMapPatterns() throws DotDataException{
		return APILocator.getContentTypeAPI2().findStructureURLMapPatterns();
	}

	/**
	 * Retrieves a list of {@link Structure} objects that the current user is
	 * allowed to access. The result set will contain all possible values,
	 * grouped by Content Type and name, and in ascendent order. Depending on
	 * the license level, some Content Types might not be included as part of
	 * the results.
	 * 
	 * @param user
	 *            - The {@link User} retrieving the list of Content Types.
	 * @param respectFrontendRoles
	 *            - If set to <code>true</code>, the permission handling will be
	 *            based on the currently logged-in user or the Anonymous role.
	 *            Otherwise, set to <code>false</code>.
	 * @param allowedStructsOnly
	 *            - If set to <code>true</code>, returns only the Content Types
	 *            the specified user has read permission on. Otherwise, set to
	 *            <code>false</code>.
	 * @return A list of permissioned {@link Structure} objects.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the
	 *             database.
	 */
	public static List<Structure> getStructures(User user, boolean respectFrontendRoles, boolean allowedStructsOnly)
			throws DotDataException {
		List<ContentType> types = APILocator.getContentTypeAPI2().findAll(user, respectFrontendRoles);
		return new ToStructureTransformer(types).asList();
	}

	/**
	 * Retrieves a list of {@link Structure} objects that the current user is
	 * allowed to access. It also allows you to have more control on the
	 * filtering criteria for the result set. Depending on the license level,
	 * some Content Types might not be included as part of the results.
	 * 
	 * @param user
	 *            - The {@link User} retrieving the list of Content Types.
	 * @param respectFrontendRoles
	 *            - If set to <code>true</code>, the permission handling will be
	 *            based on the currently logged-in user or the Anonymous role.
	 *            Otherwise, set to <code>false</code>.
	 * @param allowedStructsOnly
	 *            - If set to <code>true</code>, returns only the Content Types
	 *            the specified user has read permission on. Otherwise, set to
	 *            <code>false</code>.
	 * @param condition
	 *            - Any specific condition or filtering criteria for the
	 *            resulting Content Types.
	 * @param orderBy
	 *            - The column(s) to order the results by.
	 * @param limit
	 *            - The maximum number of records to return.
	 * @param offset
	 *            - The record offset for pagination purposes.
	 * @param direction
	 *            - The ordering of the results: <code>asc</code>, or
	 *            <code>desc</code>.
	 * @return A list of {@link Structure} objects based on the current user's
	 *         permissions and the system license.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the
	 *             database.
	 */
	public static List<Structure> getStructures(User user, boolean respectFrontendRoles, boolean allowedStructsOnly,
			String condition, String orderBy, int limit, int offset, String direction) throws DotStateException {
		
		try {
			List<ContentType> types = APILocator.getContentTypeAPI2().find(condition, orderBy, limit, offset, direction, user, false);
			return new ToStructureTransformer(types).asList();
		} catch (DotStateException | DotDataException e) {
			throw new DotStateException(e);
		}
	}
	
	public static List<Structure> getStructures()
	{
		try {
			return new ToStructureTransformer(APILocator.getContentTypeAPI2().find("1=1", "name", 10000, 0, "desc", APILocator.systemUser(), false)).asList();
		} catch (DotStateException | DotDataException e) {
			throw new DotStateException(e);
		}
	}
	
	   /**
     * Returns a list of Content Type according to a specific Type
     * These could be:
     * 1. Contents.
     * 2. Widgets.
     * 3. Forms.
     * 4. File Assets.
     * 5. Pages.
     * 6. Personas
     * @param type: Integer type, according to valid content types specified in Structure.java class
     * @return structures: List of Structures 
     */
	public static List<Structure> getAllStructuresByType(int structureType)
    {
		BaseContentType type = BaseContentType.getBaseContentType(structureType);
		try {
			return new ToStructureTransformer(APILocator.getContentTypeAPI2().findByType(type, APILocator.systemUser(),false)).asList();
		} catch (DotStateException | DotDataException | DotSecurityException e) {
			throw new DotStateException(e);
		}
    }

	public static List<Structure> getStructuresByUser(User user, String condition, String orderBy,int limit,int offset,String direction) {

		return getStructures(user,  false,  false,
				 condition,  orderBy,  limit,  offset,  direction);
	}


	public static List<Structure> getStructuresWithWritePermissions(User user, boolean respectFrontendRoles) throws DotDataException{

		try {
			
			List<Structure>  structs= new ToStructureTransformer(APILocator.getContentTypeAPI2().findAll(APILocator.systemUser(),false)).asList();
			return APILocator.getPermissionAPI().filterCollection(structs,PermissionAPI.PERMISSION_WRITE, respectFrontendRoles, user);
		
		} catch (DotStateException | DotDataException | DotSecurityException e) {
			throw new DotStateException(e);
		}
	}

	public static List<Structure> getStructuresWithReadPermissions(User user, boolean respectFrontendRoles) throws DotDataException
	{
		try {
			
			return new ToStructureTransformer(APILocator.getContentTypeAPI2().findAll(user,respectFrontendRoles)).asList();
	
		} catch (DotStateException | DotDataException e) {
			throw new DotStateException(e);
		}
	}

	public static List<Structure> getNoSystemStructuresWithReadPermissions(User user, boolean respectFrontendRoles) throws DotDataException
	{
		String orderBy = "structuretype,upper(name)";
		int limit = -1;
		String condition = " structure.system= " + DbConnectionFactory.getDBFalse();
		List<ContentType> types = APILocator.getContentTypeAPI2().find(condition, orderBy, limit, 0, "asc", user, respectFrontendRoles);
		return new ToStructureTransformer(types).asList();

	}

	public static List<Structure> getStructuresUnderHost(Host h, User user, boolean respectFrontendRoles) throws DotDataException
	{



		try{
			String condition = " host = ?";
			int limit = -1;
			List<ContentType> types = APILocator.getContentTypeAPI2().find(condition, "mod_date", limit, 0, "desc", user, respectFrontendRoles);
			return new ToStructureTransformer(types).asList();
		}
		catch(Exception e){
			Logger.error(StructureFactory.class, e.getMessage(), e);
			throw new DotDataException(e.getMessage());

		}
	}

	public static List<Structure> getStructuresByWFScheme(WorkflowScheme scheme, User user, boolean respectFrontendRoles) throws DotDataException
	{

		try{
			String condition = " structure.inode exists (select structure_id from workflow_scheme_x_structure where workflow_scheme_x_structure.scheme_id = ' "  + scheme.getId() + "')";
			int limit = -1;
			List<ContentType> types = APILocator.getContentTypeAPI2().find(condition, "name", limit, 0, "asc", user, respectFrontendRoles);
			return new ToStructureTransformer(types).asList();
		}
		catch(Exception e){
			Logger.error(StructureFactory.class, e.getMessage(), e);
			throw new DotDataException(e.getMessage());

		}
		
	}

	public static List getStructures(int limit)
	{
		String orderBy = "name";
		return getStructures(orderBy,limit);
	}

	@SuppressWarnings("unchecked")
	public static List<Structure> getStructures(String orderBy,int limit)
	{
		String direction = "asc";
		return getStructures(orderBy,limit,direction);
	}

	public static List<Structure> getStructures(String orderBy,int limit,String direction){
		return getStructures("1=1 ", "mod_date", limit, 0,direction);
		
	}

	public static List<Structure> getStructures(String condition, String orderBy,int limit,int offset,String direction) {

        //Forms are an enterprise feature...
        if ( LicenseUtil.getLevel() <= 100 ) {
            if ( !UtilMethods.isSet(condition) ) {
                condition  = " structuretype not in(" + BaseContentType.FORM.getType() + ","+BaseContentType.PERSONA.getType() +") ";
            }
            else{
            	condition += " and structuretype not in(" + BaseContentType.FORM.getType() + ","+BaseContentType.PERSONA.getType() +") ";
            }            
        }

		try{
			List<ContentType> types = APILocator.getContentTypeAPI2().find(condition, orderBy, limit, offset, direction, APILocator.systemUser(), false);
			return new ToStructureTransformer(types).asList();
		}
		catch(Exception e){
			throw new DotStateException(e);

		}
	}

	
	protected static void fixFolderHost(Structure st) {
	    if(!UtilMethods.isSet(st.getFolder())) {
	        st.setFolder(Folder.SYSTEM_FOLDER);
	    }
	    if(!UtilMethods.isSet(st.getHost())) {
	        st.setHost(Host.SYSTEM_HOST);
	    }
	}

	//### CREATE AND UPDATE
	public static void saveStructure(Structure structure) throws DotHibernateException
	{
		structure.setUrlMapPattern(cleanURLMap(structure.getUrlMapPattern()));
		Date now = new Date();
		structure.setiDate(now);
		structure.setModDate(now);
		fixFolderHost(structure);
		HibernateUtil.saveOrUpdate(structure);

		if(UtilMethods.isSet(structure.getUrlMapPattern())) {
		    CacheLocator.getContentTypeCache().clearURLMasterPattern();
		}
	}

	public static void saveStructure(Structure structure, String existingId) throws DotHibernateException
	{
		structure.setUrlMapPattern(cleanURLMap(structure.getUrlMapPattern()));
		Date now = new Date();
		structure.setiDate(now);
		structure.setModDate(now);
		fixFolderHost(structure);
		HibernateUtil.saveWithPrimaryKey(structure, existingId);
	}

	//### DELETE ###
	public static void deleteStructure(String inode) throws DotDataException
	{
		Structure structure = getStructureByInode(inode);
		deleteStructure(structure);
	}

	public static void deleteStructure(Structure structure) throws DotDataException
	{

		WorkFlowFactory wff = FactoryLocator.getWorkFlowFactory();
		wff.deleteSchemeForStruct(structure.getInode());
		InodeFactory.deleteInode(structure);
	}

	public static void disableDefault() throws DotHibernateException
	{
		Structure defaultStructure = getDefaultStructure();
		if (InodeUtils.isSet(defaultStructure.getInode()))
		{
			defaultStructure.setDefaultStructure(false);
			saveStructure(defaultStructure);
		}
	}

	public static int getTotalDates(Structure structure)
	{
		String typeField = Field.FieldType.DATE.toString();
		int intDate = getTotals(structure,typeField);
		typeField = Field.FieldType.DATE_TIME.toString();
		int intDateTime = getTotals(structure,typeField);
		return intDate + intDateTime;
	}

	public static int getTotalImages(Structure structure)
	{
		String typeField = Field.FieldType.IMAGE.toString();
		return getTotals(structure,typeField);
	}

	public static int getTotalFiles(Structure structure)
	{
		String typeField = Field.FieldType.FILE.toString();
		return getTotals(structure,typeField);
	}

	public static int getTotalTextAreas(Structure structure)
	{
		String typeField = Field.FieldType.TEXT_AREA.toString();
		return getTotals(structure,typeField);
	}

	public static int getTotalWYSIWYG(Structure structure)
	{
		String typeField = Field.FieldType.WYSIWYG.toString();
		return getTotals(structure,typeField);
	}

	public static int getTotals(Structure structure,String typeField)
	{
		List fields = structure.getFields();
		int total = 0;
		for(int i = 0; i < fields.size();i++)
		{
			Field field = (Field) fields.get(i);
			if(field.getFieldType().equals(typeField))
			{
				total++;
			}
		}
		return total;
	}

	/**
	 * Create the default cms contentlet structure
	 * @throws DotHibernateException
	 *
	 */
	public static void createDefaultStructure () throws DotHibernateException {
		Structure st = StructureFactory.getDefaultStructure();
		if (st == null || !InodeUtils.isSet(st.getInode())) {

			Logger.info(StructureFactory.class, "Creating the default cms contentlet structure");

			st.setDefaultStructure(true);
			st.setFixed(false);
			st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
			st.setDescription("Default CMS Content Structure");
			st.setName("Web Page Content");
			StructureFactory.saveStructure(st);

			Field field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("text1");
			field.setFieldName("Title");
			field.setFieldType(Field.FieldType.TEXT.toString());
			field.setHint("");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(0);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setVelocityVarName("ContentletTitle");
			field.setIndexed(true);
			field.setSearchable(true);
			field.setListed(true);
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);


			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("text_area1");
			field.setFieldName("Body");
			field.setFieldType(Field.FieldType.WYSIWYG.toString());
			field.setHint("");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(8);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setIndexed(true);
			field.setVelocityVarName("Body");
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);



			/* Let's create a News Items structure as well */

			st = new Structure();
			st.setName("News Item");
			st.setDescription("News Items and Press Releases");
			st.setFixed(false);
			st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
			StructureFactory.saveStructure(st);

			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("text1");
			field.setFieldName("Headline");
			field.setFieldType(Field.FieldType.TEXT.toString());
			field.setHint("");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(0);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setVelocityVarName("NewsHeadline");
			field.setIndexed(true);
			field.setListed(true);
			field.setSearchable(true);
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);

			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("text_area1");
			field.setFieldName("Short Summary");
			field.setFieldType(Field.FieldType.TEXT_AREA.toString());
			field.setHint("This is teaser copy shown on listing pages");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(1);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setVelocityVarName("NewsSummary");
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);

			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("date1");
			field.setFieldName("Publish Date");
			field.setFieldType(Field.FieldType.DATE_TIME.toString());
			field.setHint("<br>The date this news item will be displayed");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(2);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setVelocityVarName("NewsPublishDate");
			field.setListed(true);
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);

			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("date2");
			field.setFieldName("Expire Date");
			field.setFieldType(Field.FieldType.DATE_TIME.toString());
			field.setHint("<br>The date this item will expire");
			field.setRegexCheck("");
			field.setRequired(false);
			field.setSortOrder(3);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setVelocityVarName("NewsExpireDate");
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);


			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("text_area2");
			field.setFieldName("Body");
			field.setFieldType(Field.FieldType.WYSIWYG.toString());
			field.setHint("");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(4);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setIndexed(true);
			field.setVelocityVarName("NewsBody");
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);




		}
	}

	/**
	 * Gets the list of fields of a structure which type it is TAG
	 * @param structureInode inode of the structure owner of the fields to get
	 * @return a list of fields of a structure which type it is TAG
	 */
	public static ArrayList<Field> getTagsFields(String structureInode) {
		ArrayList<Field> tagFields = new ArrayList<Field>();
		List<Field> listFields = FieldsCache.getFieldsByStructureInode(structureInode);

		for (Field f : listFields) {
			if (f.getFieldType().equals(Field.FieldType.TAG.toString())) {
				tagFields.add(f);
			}
		}

		return tagFields;
	}

    /**
     * Counts the amount of structures in DB filtering by the given condition
     * 
     * @param condition to be used
     * @return Amount of structures found
     */
    public static int getStructuresCount(String condition) {
        DotConnect db = new DotConnect();

        StringBuffer sb = new StringBuffer();

        condition = (UtilMethods.isSet(condition.trim())) ? condition + " AND " : "";
        if (LicenseUtil.getLevel() < 200) {
            condition += " structuretype NOT IN (" + Structure.STRUCTURE_TYPE_FORM + ", "
                    + Structure.STRUCTURE_TYPE_PERSONA + ") AND ";
        }

        condition += " 1=1 ";

        try {

            sb.append("select count(distinct structure.inode ) as count ");
            sb.append(" from structure ");
            if (condition != null && UtilMethods.isSet(condition)) {
                sb.append(" where " + condition);
            }
            Logger.debug(StructureFactory.class, sb.toString());
            db.setSQL(sb.toString());
            return db.getInt("count");

        } catch (Exception e) {
            Logger.error(WebAssetFactory.class, "getStructuresCount failed:" + e, e);
        }
        return 0;
    }

	/**
	 * Get the list of image fields of a structure having a value in a list of parameters
	 * @param structure The structure whose fields will be compared to the list of values given
	 * @param parametersName A list with the velocity name of the fields to be compared to the structure
	 * @param values A list with the values of the fields to be compared to the structure
	 * @return List<Field>
	 */
	public static List<Field> getImagesFieldsList(Structure structure, List<String> parametersName, List<String[]> values){
		List<Field> imageList = new ArrayList<Field>();
		for(int i=0; i < parametersName.size(); i++){
			String fieldname = parametersName.get(i);
			String[] fieldValue = values.get(i);
			Field field = structure.getFieldVar(fieldname);
			if(UtilMethods.isSet(field) && APILocator.getFieldAPI().valueSettable(field)){
				if(field.getFieldType().equals(Field.FieldType.IMAGE.toString()) && UtilMethods.isSet(fieldValue)){
					imageList.add(field);
				}
			}
		}
		return imageList;
	}

	/**
	 * Get the list of file fields of a structure having a value in a list of parameters
	 * @param structure The structure whose fields will be compared to the list of values given
	 * @param parametersName A list with the velocity name of the fields to be compared to the structure
	 * @param values A list with the values of the fields to be compared to the structure
	 * @return List<Field>
	 */
	public static List<Field> getFilesFieldsList(Structure structure, List<String> parametersName, List<String[]> values){
		List<Field> fileList = new ArrayList<Field>();
		for(int i=0; i < parametersName.size(); i++){
			String fieldname = parametersName.get(i);
			String[] fieldValue = values.get(i);
			Field field = structure.getFieldVar(fieldname);
			if(UtilMethods.isSet(field) && APILocator.getFieldAPI().valueSettable(field)){
				if(field.getFieldType().equals(Field.FieldType.FILE.toString()) && UtilMethods.isSet(fieldValue)){
					fileList.add(field);
				}
			}
		}
		return fileList;
	}

	public static List<Map<String, Serializable>> DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,DotDataException {

		Map<String, String> dbColToObjectAttribute = new HashMap<String, String>();

		if(UtilMethods.isSet(query.getSelectAttributes())){

			if(!query.getSelectAttributes().contains("name")){
				query.getSelectAttributes().add("name" + " as " + QueryResult.CMIS_TITLE);
			}
		}else{
			List<String> atts = new ArrayList<String>();
			atts.add("*");
			atts.add("name" + " a" +
					"s " + QueryResult.CMIS_TITLE);
			query.setSelectAttributes(atts);
		}

		return QueryUtil.DBSearch(query, dbColToObjectAttribute, null, user, true, respectFrontendRoles);
	}

	private static String cleanURLMap(String urlMap){
		if(!UtilMethods.isSet(urlMap)){
			return null;
		}
		urlMap = urlMap.trim();
		if(!urlMap.startsWith("/")){
			urlMap = "/" + urlMap;
		}
		return urlMap;
	}

	public static void updateFolderReferences(Folder folder) throws DotDataException{

		HibernateUtil dh = new HibernateUtil(Structure.class);
        dh.setQuery("select inode from inode in class " + Structure.class.getName() + " where inode.folder = ?");
	    dh.setParam(folder.getInode());
		List<Structure> results = dh.list();
		for(Structure  structure : results){
			if(UtilMethods.isSet(folder.getHostId()) && !hostAPI.findSystemHost().getIdentifier().equals(folder.getHostId())){
				structure.setHost(folder.getHostId());
			}else{
				structure.setHost("SYSTEM_HOST");
			}
			structure.setFolder("SYSTEM_FOLDER");
			HibernateUtil.saveOrUpdate(structure);
			CacheLocator.getContentTypeCache().remove(structure);
			permissionAPI.resetPermissionReferences(structure);

		}

	}

	public static void updateFolderFileAssetReferences(Structure st) throws DotDataException{
		Structure defaultFileAssetStructure = getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
		if(defaultFileAssetStructure!=null &&
				UtilMethods.isSet(defaultFileAssetStructure.getInode())){
			DotConnect dc = new DotConnect();
			dc.setSQL("update folder set default_file_type = ? where default_file_type = ?");
			dc.addParam(defaultFileAssetStructure.getInode());
			dc.addParam(st.getInode());
			dc.loadResult();
		}

	}

	public static List<Structure> findStructuresUserCanUse(User user, String query, Integer structureType, int offset, int limit) throws DotDataException, DotSecurityException {
		return PermissionedWebAssetUtil.findStructuresForLimitedUser(query, structureType, "name", offset, limit, PermissionAPI.PERMISSION_READ, user, false);
	}


}

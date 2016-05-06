package guessthenextword.knowledge_base;

import guessthenextword.util.Configuration;
import guessthenextword.util.Logger;

import java.lang.reflect.Constructor;
import java.util.Properties;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public final class KnowledgeBases {
	
	//==> Methods

	public static KnowledgeBase getKnowledgeBase( String kbType, String[] kbArgs ){
		KnowledgeBase knowledgeBase = null;
		try{
			Class<?> c = Class.forName("guessthenextword.knowledge_base."+kbType+"KnowledgeBase");
			Class<? extends KnowledgeBase> m = c.asSubclass(KnowledgeBase.class);
			Constructor<? extends KnowledgeBase> constr = m.getConstructor(kbArgs.getClass());
			knowledgeBase = constr.newInstance(new Object[]{kbArgs});
		}catch(Exception e){
			Logger.log("KnowledgeBases", "Error while loading the "+kbType+"-based Knowledge Base.\n Reason: \n \t "+e.toString());
			System.exit(-1);
		}
		return knowledgeBase;
	}//getKnowledgeBase
	
	public static KnowledgeBase getDefaultKnowledgeBase(){
		//load the knowledge bases properties
		Properties properties = Configuration.getConfiguration();
		String kbType = properties.getProperty("kbType");
		String kbArguments = properties.getProperty("kbArguments", "") + ",READ_ONLY";
		String[] kbArgs = kbArguments.split(",");
		if( kbType == null ){
			throw new IllegalArgumentException("Configuration File Error: The configuration variable 'kbType' must be set");
		}
		return getKnowledgeBase(kbType, kbArgs);
	}//getDefaultKnowledgeBase
	
	public static EditableKnowledgeBase getEditableKnowledgeBase( String kbType, String[] kbArgs ){
		EditableKnowledgeBase knowledgeBase = null;
		try{
			Class<?> c = Class.forName("guessthenextword.knowledge_base."+kbType+"KnowledgeBase");
			Class<? extends EditableKnowledgeBase> m = c.asSubclass(EditableKnowledgeBase.class);
			Constructor<? extends EditableKnowledgeBase> constr = m.getConstructor(kbArgs.getClass());
			knowledgeBase = constr.newInstance(new Object[]{kbArgs});
		}catch(Exception e){
			Logger.log("KnowledgeBases", "Error while loading the "+kbType+"-based Editable Knowledge Base.");
		}
		return knowledgeBase;
	}//getEditableKnowledgeBase
	
	public static EditableKnowledgeBase getDefaultEditableKnowledgeBase(){
		//load the knowledge bases properties
		Properties properties = Configuration.getConfiguration();
		String kbType = properties.getProperty("kbType");
		String kbArguments = properties.getProperty("kbArguments");
		String[] kbArgs = (kbArguments == null)? new String[]{} : kbArguments.split(",");
		if( kbType == null ){
			throw new IllegalArgumentException("Configuration File Error: The configuration variable 'kbType' must be set");
		}
		return getEditableKnowledgeBase(kbType, kbArgs);
	}//getDefaultEditableKnowledgeBase
	
}//KnowledgeBases

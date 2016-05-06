package guessthenextword.compiler;

import guessthenextword.compiler.similarity.JaccardSimilarity;
import guessthenextword.compiler.similarity.Similarity;
import guessthenextword.compiler.sources.DataSource;
import guessthenextword.dictionary.CustomFileDictionary;
import guessthenextword.dictionary.Dictionary;
import guessthenextword.dictionary.FullDictionary;
import guessthenextword.dictionary.WordNetDictionary;
import guessthenextword.knowledge_base.EditableKnowledgeBase;
import guessthenextword.knowledge_base.KnowledgeBases;
import guessthenextword.structures.Sentence;
import guessthenextword.util.BufferedConcurrentFileWriter;
import guessthenextword.util.Configuration;
import guessthenextword.util.Formatter;
import guessthenextword.util.Logger;
import guessthenextword.util.Statistics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class KnowldgeBaseCompiler {

	//==> Fields
	
	public boolean interruptionRequested = false;

	private static int bufferMaxLength = 10000;
	private static double maxFilesPerThread = 100;

	private static final String defaultSimilarityMeasureName = "Jaccard";
	private Similarity similarityModule;
	private List<DataSource> sources;
	private EditableKnowledgeBase knowledgeBase;
	private String lastSentenceID = null;
	private List<String> processedFiles = null;
	private boolean existsPartialData = false;
	private File step1FlagFile;
	private Properties step1 = new Properties();
	private File step2FlagFile;
	private Properties step2 = new Properties();
	private File step3FlagFile;
	private Properties step3 = new Properties();
	private File step4FlagFile;
	private Properties step4 = new Properties();
	private int relaxationPeriodLength = 10000; //Milliseconds
	
	private final int defaultServerPort = 14000;
	private boolean serverMode = false;
	private boolean guiMode = true;
	
	private CommandReceiver commandReceiver;

	private String tempFolder = null;

	private final int CPUs = Runtime.getRuntime().availableProcessors();
	private ExecutorService executor;

	private int percentage = 0;
	private long timeMillis = 0;

	public Semaphore bufferSem = new Semaphore(bufferMaxLength, false);

	private final String step2ValidLineRegEx = "(\\w+) \\| ([0-9]+)";
	private final Pattern step2ValidLinePattern = Pattern.compile(step2ValidLineRegEx);

	private FullDictionary dictionary = new FullDictionary();
	private Properties properties = Configuration.getConfiguration();



	//==> Constructors

	protected KnowldgeBaseCompiler(){}
	
	public KnowldgeBaseCompiler (String similarityMeasureName, int serverPort, boolean guiMode){
		this(similarityMeasureName, serverPort);
		this.guiMode = guiMode;
	}//KnowldgeBaseCompiler
	
	public KnowldgeBaseCompiler (String similarityMeasureName, int serverPort){
		this(similarityMeasureName);
		//
		if( serverPort >= 0 && serverPort < 65536 ){
			if( serverPort == 0 ){
				serverPort = defaultServerPort;
			}
			serverMode = true;
			//open the log server
			Statistics.init(serverPort);
			Logger.log(this.getClass().getSimpleName(), "Server mode activated on port "+serverPort+".");
		}else{
			serverMode = false;
			Logger.log(this.getClass().getSimpleName(), "Standalone mode activated!");
		}
		this.guiMode = !serverMode;
	}//KnowldgeBaseCompiler

	public KnowldgeBaseCompiler (String similarityMeasureName){
		//load dictionaries
		WordNetDictionary wnd = new WordNetDictionary();
		dictionary.addDictionary( wnd.getDictionary() );
		try{
			String customDictsNumString = properties.getProperty("customFiles");
			int customDictsNum = Integer.parseInt(customDictsNumString);
			//for each dictionary
			for( int i=1; i <= customDictsNum; i++ ){
				CustomFileDictionary cfd = new CustomFileDictionary(i);
				cfd.loadDictionary();
				dictionary.addDictionary( cfd.getDictionary() );
			}
		}catch(Exception e){
			Logger.log(this.getClass().getSimpleName(), "Error while loading the dictionaries. \n Reason: \n "+e.toString());
		}
		Logger.log(this.getClass().getSimpleName(), "All dictionaries successfully loaded!");
		Logger.log(this.getClass().getSimpleName(), "Dictionary size: "+dictionary.size()+" elements \n");
		//instantiate the knowledge base
		knowledgeBase = KnowledgeBases.getDefaultEditableKnowledgeBase();
		if( knowledgeBase == null ) System.exit(-1);
		//create the sources list
		sources = new LinkedList<DataSource>();
		//read the tempFolder properties
		tempFolder = properties.getProperty("tempFolder");
		tempFolder = ( tempFolder == null )? /*Default*/ "temp"+File.separator : tempFolder;
		tempFolder = ( tempFolder.charAt(tempFolder.length()-1) == File.separatorChar )? tempFolder : tempFolder+File.separator;
		//build the temporary folder structure
		buildFolderStructure();
		//load partial results
		try{
			step1FlagFile = new File(tempFolder+"flags"+File.separator+"step1.flag");
			if( step1FlagFile.exists() ){
				step1.load( new FileInputStream( step1FlagFile ) );
				//
				lastSentenceID = step1.getProperty("lastSentenceID");
				String files = step1.getProperty("processedFiles");
				//
				if( files != null ){
					processedFiles = new LinkedList<String>();
					for( String filename : files.split(",") ){
						processedFiles.add(filename);
						Logger.log(this.getClass().getSimpleName(), "Partial data found for file: "+filename);
					}
				}
			}
			if(lastSentenceID != null && processedFiles != null){
				existsPartialData = true;
			}
		}catch(Exception e){
			//there is no partial data to load
		}
		try{
			step2FlagFile = new File(tempFolder+"flags"+File.separator+"step2.flag");
			if( step2FlagFile.exists() ){
				step2.load( new FileInputStream( step2FlagFile ) );
			}
		}catch(Exception e){
			//there is no partial data to load
		}
		try{
			step3FlagFile = new File(tempFolder+"flags"+File.separator+"step3.flag");
			if( step3FlagFile.exists() ){
				step3.load( new FileInputStream( step3FlagFile ) );
			}
		}catch(Exception e){
			//there is no partial data to load
		}
		try{
			step4FlagFile = new File(tempFolder+"flags"+File.separator+"step4.flag");
			if( step4FlagFile.exists() ){
				step4.load( new FileInputStream( step4FlagFile ) );
			}
		}catch(Exception e){
			//there is no partial data to load
		}
		if( similarityMeasureName != null ){
			//try to create the specific similarity measure object
			try{
				Class<?> c = Class.forName("guessthenextword.compiler.similarity."+similarityMeasureName+"Similarity");
				Method method = c.getMethod("getInstance", new Class[0] );

				Object o = method.invoke(null, new Object[0]);
				if( o instanceof Similarity ){
					similarityModule = (Similarity)o;
					Logger.log(this.getClass().getSimpleName(), "Custom Similarity Module loaded: "+similarityMeasureName);
				}
			}catch(Exception e){}
			if(similarityModule == null){
				Logger.log(this.getClass().getSimpleName(), "Error occurred while loading the specified Similarity Module: "+similarityMeasureName);
				Logger.log(this.getClass().getSimpleName(), "Default Similarity Module loaded: Jaccard");
				//create the default similarity module
				similarityMeasureName = defaultSimilarityMeasureName;
				similarityModule = JaccardSimilarity.getInstance(); //<= Default
			}
		}else{
			Logger.log(this.getClass().getSimpleName(), "Default Similarity Module loaded: Jaccard");
			//create the default similarity module
			similarityMeasureName = defaultSimilarityMeasureName;
			similarityModule = JaccardSimilarity.getInstance(); //<= Default
		}
		//update the knowledgebase informations
		knowledgeBase.setDefaultSimilarityType(similarityMeasureName);
		//open the command receiver server
		commandReceiver = new CommandReceiver();
		commandReceiver.start();
	}//KnowldgeBaseCompiler



	//==> Methods

	public boolean isSourceAvailable( String sourceName, String sourceRegex ) {
		try{
			Class<?> c = Class.forName("guessthenextword.compiler.sources."+sourceName+"DataSource");
			Class<? extends DataSource> m = c.asSubclass(DataSource.class);

			m.getConstructor(File[].class);

			return (getSourceFiles(sourceRegex) != null);
		}catch(Exception e){
			return false;
		}
	}//isSourceAvailable

	public void addSource( String sourceName, String sourceRegex ) {
		if( isSourceAvailable(sourceName, sourceRegex) ){
			try{
				File[] files = getSourceFiles(sourceRegex);

				Class<?> c = Class.forName("guessthenextword.compiler.sources."+sourceName+"DataSource");
				Class<? extends DataSource> m = c.asSubclass(DataSource.class);

				Constructor<? extends DataSource> constr = m.getConstructor(File[].class);
				DataSource source = constr.newInstance(new Object[] { files });

				sources.add(source);
			}catch(Exception e){
				//Do nothing!
			}
		}
	}//addSource

	public void buildKB() {
		//initialize the statistics
		Statistics.init();
		if( guiMode ){
			Statistics.generateGUI(400);
		}
		//
		BufferedConcurrentFileWriter bfw = BufferedConcurrentFileWriter.getInstance();
		//== STEP 1 : 	For each '<word>' of each sentence, append the '<sentenceID>' to the '<word>.dat' file
		//				For each sentence with <sentenceID> = 'XYZ1234', append the sentence to the 'X/Y/Z.dat' file
		String lastSentenceID = step1.getProperty("lastSentenceID");
		ConcurrentSentenceIDProvider.setLastSentenceID(lastSentenceID);
		String completed = step1.getProperty("completed");
		Statistics.note("currentStep", "1/4");
		Statistics.note("maxThreadBufferLength", bufferMaxLength);
		if( completed == null || !completed.equals("1") ){
			//
			Logger.log(this.getClass().getSimpleName(), "Step 1/4: Compiling...");
			//launch the executor
			executor = Executors.newFixedThreadPool( CPUs );
			//for each data-source
			for( DataSource source : sources ){
				for( int i = 0; i < source.getSourcesNum(); i++){
					//read the filename
					String filename = source.getSourceName(i);
					//read the source data
					source.selectSource(i);
					Iterator<Sentence> sit = source.iterator();
					Statistics.note("currentFile", filename);
					while(sit.hasNext()){
						//for each sentence
						Sentence s = sit.next();
						try{
							bufferSem.acquire();
							//
							Statistics.increaseInt("threadBufferLength");
							executor.execute( new StepOneWorker(this, s, dictionary, tempFolder) );
							//
						}catch(InterruptedException ie){}
					}
					//end of file
					String processedFiles = step1.getProperty("processedFiles");
					processedFiles = (processedFiles == null)? "" : processedFiles;
					step1.setProperty("processedFiles", processedFiles+","+filename);
					try {
						step1.store(new FileOutputStream(step1FlagFile), null);
					} catch (Exception e) { /*do nothing*/ }
					//relaxation period
					Logger.log(this.getClass().getSimpleName(), "Relaxation period: "+(relaxationPeriodLength/1000)+" sec");
					try {
						Runnable r = new Runnable() {
							private int counter = relaxationPeriodLength;
							@Override
							public void run() {
								while(counter > 0){
									Statistics.note("fileRemainingTime", counter/1000+"s");
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {}
									finally{
										counter = counter - 1000;
									}
								}
							}
						};
						new Thread(r).start();
						Thread.sleep(relaxationPeriodLength);
					} catch (InterruptedException e) { /*do nothing*/}
				}
			}
			//close the executor
			executor.shutdown();
			while( !executor.isTerminated() ){
				try {
					executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) { /*do nothing*/ }
			}
			//flush the buffered file writer
			bfw.flush();
			//store the last used sentenceID and the 'completed' flag
			lastSentenceID = ConcurrentSentenceIDProvider.getLastSentenceID();
			step1.setProperty("completed", "1");
			step1.setProperty("lastSentenceID", lastSentenceID);
			try {
				step1.store(new FileOutputStream(step1FlagFile), null);
			} catch (Exception e) { /*do nothing*/ }
			//
			Logger.log(this.getClass().getSimpleName(), "Step 1/4: Completed!\n");
		}else{
			Logger.log(this.getClass().getSimpleName(), "Step 1/4: Pre-Processed Data Loaded!\n");
		}
		//
		//
		//== STEP 2 : 	For each temporary '<wordA>.dat' file, count the corresponding sentences in
		//				which it occurs and store these informations into the '<firstChar>.dat' file
		Map<String,Integer> wordOccur = new ConcurrentHashMap<String,Integer>();
		completed = step2.getProperty("completed");
		Statistics.note("currentStep", "2/4");
		if( completed == null || !completed.equals("1") ){
			//
			Logger.log(this.getClass().getSimpleName(), "Step 2/4: Compiling...");
			//launch the executor
			executor = Executors.newFixedThreadPool( CPUs );
			//read the folder structure of the step1 folder
			File step1WordsFolder = new File( tempFolder+"step1"+File.separator+"words" );
			final List<String> processedChars = Arrays.asList((step2.getProperty("processedChars", "")).split(","));
			if( step1WordsFolder.exists() && step1WordsFolder.isDirectory() ){
				//create a specific fileNameFilter object
				FilenameFilter step1WordsFolderFilter = new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return (!processedChars.contains(name)) && (name.matches("[a-z]")) && (new File(dir.getAbsolutePath()+File.separator+name).isDirectory());
					}
				};
				List<String> wordsFolders = Arrays.asList( step1WordsFolder.list(step1WordsFolderFilter) );
				Collections.sort(wordsFolders);
				//create a specific fileNameFilter object
				FilenameFilter step1WordsFilesFilter = new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return (name.matches("(\\w+).dat")) && !(new File(dir.getAbsolutePath()+File.separator+name).isDirectory());
					}
				};
				//compute the number of files we have to process
				long totalWords = 0;
				for( String folder : wordsFolders ){
					File wordsFolder = new File( tempFolder+"step1"+File.separator+"words"+File.separator+folder );
					totalWords += wordsFolder.list(step1WordsFilesFilter).length;
				}
				long currentWord = 0;
				//for each '[a-z]' folder
				for( String folder : wordsFolders ){
					File wordsFolder = new File( tempFolder+"step1"+File.separator+"words"+File.separator+folder );
					List<String> words = Arrays.asList( wordsFolder.list(step1WordsFilesFilter) );
					//for each '<word>.dat' file
					for( String word : words ){
						currentWord++;
						File file = new File( tempFolder+"step1"+File.separator+"words"+File.separator+folder+File.separator+word );
						Statistics.note("currentFile", word);
						//==>
						try{
							bufferSem.acquire();
							//
							Statistics.increaseInt("threadBufferLength");
							executor.execute( new StepTwoWorker(this, file, tempFolder, wordOccur) );
							//
						}catch(InterruptedException ie){}
						//<==
						sendProgressDataToLogger(totalWords, currentWord);
					}
					//'folder' completed
					step2.setProperty("processedChars", step2.getProperty("processedChars")+","+folder);
				}
				//
				step2.setProperty("completed", "1");
			}
			//close the executor
			executor.shutdown();
			while( !executor.isTerminated() ){
				try {
					executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) { /*do nothing*/ }
			}
			//flush the buffered file writer
			bfw.flush();
			//store the step2 properties
			try {
				step2.store(new FileOutputStream(step2FlagFile), null);
			} catch (Exception e) { /*do nothing*/ }
			//
			Logger.log(this.getClass().getSimpleName(), "Step 2/4: Completed!\n");
		}else{
			//try to load the pre-processed data
			//create a specific fileNameFilter object
			FilenameFilter step1WordsFilesFilter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return (name.matches("\\w.dat")) && !(new File(dir.getAbsolutePath()+File.separator+name).isDirectory());
				}
			};
			File wordsFolder = new File( tempFolder+"step2"+File.separator);
			List<String> files = Arrays.asList( wordsFolder.list(step1WordsFilesFilter) );
			Collections.sort(files);
			//for each file
			Matcher m;
			BufferedReader br;
			for( String fileName : files ){
				File file = new File( tempFolder+"step2"+File.separator+fileName );
				try {
					br = new BufferedReader( new FileReader( file ) );
					String line = br.readLine();
					while( line != null ){
						m = step2ValidLinePattern.matcher(line);
						if( m.find() ){
							wordOccur.put( m.group(1) , Integer.parseInt(m.group(2).trim()) );
						}
						//get the next line
						line = br.readLine();
					}
					//close the reader
					br.close();
				} catch (Exception e) { /* do nothing! */ }
			}
			//
			Logger.log(this.getClass().getSimpleName(), "Step 2/4: Pre-Processed Data Loaded!\n");
		}
		//== STEP 3 : 	For each temporary '<sentences>.dat' file, load the lines and sort them
		//				alphabetically, then rewrite the file.
		completed = step3.getProperty("completed");
		Statistics.note("currentStep", "3/4");
		if( completed == null || !completed.equals("1") ){
			//
			Logger.log(this.getClass().getSimpleName(), "Step 3/4: Compiling...");
			//launch the executor
			executor = Executors.newFixedThreadPool( CPUs );
			//read the folder structure of the step1 folder
			final List<String> processedChars = Arrays.asList((step3.getProperty("processedChars", "")).split(","));
			String step1SentencesFolder = tempFolder+"step1"+File.separator+"sentences"+File.separator;
			List<String> list = new LinkedList<String>();
			String newProcessedChars = "";
			for(char alphabet = 'A'; alphabet <= 'Z'; alphabet++) {
				if( !processedChars.contains(alphabet+"") ){
					list.add(alphabet+File.separator);
					newProcessedChars += ","+alphabet;
				}
			}
			//
			int levels = ConcurrentSentenceIDProvider.sentencePrefixLength-1;
			int currentLevel = 0;
			while( currentLevel < levels ){
				currentLevel++;
				//
				List<String> newList = new LinkedList<String>();
				Iterator<String> it = list.iterator();
				while(it.hasNext()){
					String s = it.next();
					//
					for(char alphabet = 'A'; alphabet <= 'Z'; alphabet++) {
						String ext = (currentLevel == levels)? ".dat" : File.separator;
						newList.add(s+alphabet+ext);
					}
					//
					it.remove();
				}
				//swap
				list = newList;
			}
			//prune the useless strings
			Iterator<String> it = list.iterator();
			while(it.hasNext()){
				if( !new File(step1SentencesFolder+it.next()).exists() ){
					it.remove();
				}
			}
			//reset the buffer max length for this new job
			bufferMaxLength = 100;
			Statistics.note("maxThreadBufferLength", bufferMaxLength );
			bufferSem = new Semaphore(bufferMaxLength, false);
			//launch the sorting threads
			int totalFiles = list.size(), currentFile = 0;
			for( String localFilePath : list ){
				currentFile++;
				//==>
				try{
					bufferSem.acquire();
					//
					Statistics.increaseInt("threadBufferLength");
					executor.execute( new StepThreeWorker(this, localFilePath, tempFolder, step1SentencesFolder) );
					//
				}catch(InterruptedException ie){}
				//<==
				Statistics.note("currentFile", localFilePath);
				Statistics.note("currentJob", localFilePath.replaceFirst("[.][^.]+$", "").replaceAll(File.separator, ""));
				sendProgressDataToLogger(totalFiles, currentFile);
			}
			//
			step3.setProperty("processedChars", step3.getProperty("processedChars")+newProcessedChars);
			step3.setProperty("completed", "1");
			//close the executor
			executor.shutdown();
			while( !executor.isTerminated() ){
				try {
					executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) { }
			}
			//store the step3 properties
			try {
				step3.store(new FileOutputStream(step3FlagFile), null);
			} catch (Exception e) { }
			//
			Logger.log(this.getClass().getSimpleName(), "Step 3/4: Completed!\n");
		}else{
			Logger.log(this.getClass().getSimpleName(), "Step 3/4: Pre-Processed Data Loaded!\n");
		}
		//== STEP 4 : 	For each temporary '<wordA>.dat' file, load the corresponding sentences
		//				For each <wordB> in that sentences, compute the <wordA,wordB> similarity
		completed = step4.getProperty("completed");
		Statistics.note("currentStep", "4/4");
		if( completed == null || !completed.equals("1") ){
			if( guiMode ){
				Statistics.openExtraGUI();
			}
			//
			Logger.log(this.getClass().getSimpleName(), "Step 4/4: Compiling...");
			//launch the executor
			executor = Executors.newFixedThreadPool( CPUs );
			//read the folder structure of the step1 folder
			File step1WordsFolder = new File( tempFolder+"step1"+File.separator+"words" );
			final List<String> processedChars = Arrays.asList((step4.getProperty("processedChars", "")).split(","));
			if( step1WordsFolder.exists() && step1WordsFolder.isDirectory() ){
				//create a specific fileNameFilter object
				FilenameFilter step1WordsFolderFilter = new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return (!processedChars.contains(name)) && (name.matches("[a-z]")) && (new File(dir.getAbsolutePath()+File.separator+name).isDirectory());
					}
				};
				List<String> wordsFolders = Arrays.asList( step1WordsFolder.list(step1WordsFolderFilter) );
				Collections.sort(wordsFolders);
				//create a specific fileNameFilter object
				FilenameFilter step1WordsFilesFilter = new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return (name.matches("(\\w+).dat")) && !(new File(dir.getAbsolutePath()+File.separator+name).isDirectory());
					}
				};
				//compute the number of files we have to process
				long totalWords = 0;
				for( String folder : wordsFolders ){
					File wordsFolder = new File( tempFolder+"step1"+File.separator+"words"+File.separator+folder );
					totalWords += wordsFolder.list(step1WordsFilesFilter).length;
				}
				//Optimization subStep: exclude stopwords
				Dictionary stopWords = null;
				boolean stopWordsExclusion = properties.getProperty("stopWordExclusion", "false").equals("true");
				if( stopWordsExclusion ){
					Logger.log(this.getClass().getSimpleName(), "Stop Words Exclusion Enabled!");
					String stopWordsPath = properties.getProperty("stopWordsPath", "");
					String stopWordsRegEx =  properties.getProperty("stopWordsRegEx", "(\\w+)\\s+");
					String stopWordsGroups =  properties.getProperty("stopWordsGroups", "1");
					stopWords = new CustomFileDictionary(stopWordsPath, stopWordsRegEx, stopWordsGroups, "StopWordsDictionary");
				}else{
					Logger.log(this.getClass().getSimpleName(), "Stop Words Exclusion Disabled!");
				}
				//Optimization subStep: exclude useless words
				FullDictionary dict = new FullDictionary();
				boolean wordsFiltering = properties.getProperty("filterWordsByCustomDictionaries", "false").equals("true");
				if( wordsFiltering ){
					Logger.log(this.getClass().getSimpleName(), "Words Filtering Enabled!");
					String[] customDictIDs = properties.getProperty("filterCustomDictionaries", "").split(",");
					for( String s : customDictIDs ){
						try{
							Integer i = Integer.parseInt(s);
							dict.addDictionary( new CustomFileDictionary(i).getDictionary() );
						}catch(Exception e){}
					}
				}else{
					Logger.log(this.getClass().getSimpleName(), "Words Filtering Disabled!");
				}
				//reset the buffer max length for this new job
				bufferMaxLength = 100;
				Statistics.note("maxThreadBufferLength", bufferMaxLength );
				bufferSem = new Semaphore(bufferMaxLength, false);
				//process the data
				long currentWord = 0;
				//for each '[a-z]' folder
				for( String folder : wordsFolders ){
					if( interruptionRequested ){
						break;
					}
					//
					File wordsFolder = new File( tempFolder+"step1"+File.separator+"words"+File.separator+folder );
					List<String> words = Arrays.asList( wordsFolder.list(step1WordsFilesFilter) );
					//for each '<word>.dat' file
					for( String word : words ){
						if( interruptionRequested ){
							break;
						}
						//
						currentWord++;
						Statistics.note("currentFile", word);
						Statistics.note("currentJob", "File "+currentWord+" of "+totalWords);
						//
						File file = new File( tempFolder+"step1"+File.separator+"words"+File.separator+folder+File.separator+word );
						String wordName = word.trim().replaceFirst("[.][^.]+$", ""); //<= remove the extension
						//words filtering subStep
						if( !wordsFiltering || dict.contains(wordName) ){
							//partial data exclusion subStep
							if( !knowledgeBase.isAvailable(wordName) ){
								//stopWords exclusion subStep
								if( !stopWordsExclusion || !stopWords.getDictionary().contains(wordName.trim()) ){
									Map<String,List<String>>[] threadMaps = readWordFile(file, wordOccur);
									Statistics.note(wordName+"WorkersCounter", threadMaps.length);
									//partial data map and semaphore
									Map<String,Integer> partialData = new HashMap<String,Integer>();
									Semaphore partialDataSem = new Semaphore(1,true);
									//for each thread job
									for( int i=0; i<threadMaps.length; i++ ){
										//==>
										try{
											bufferSem.acquire();
											//
											Statistics.increaseInt("threadBufferLength");
											executor.execute( new StepFourWorker(this, threadMaps[i], i, threadMaps.length, word, tempFolder, wordOccur, partialData, partialDataSem, knowledgeBase, similarityModule) );
											//
										}catch(InterruptedException ie){}
										//<==
									}
								}else{
									//increase the excluded words counter
									Statistics.increaseInt("excludedFilesCount");
									//increase the excluded words total size
									int fileSize = (int) file.length();
									Statistics.increaseInt("excludedFilesSize", fileSize);
								}
							}else{
								//Logger.log(this.getClass().getSimpleName(), "Step 4/4: Partial data found for: "+word );
							}
						}else{
							//useless word => skip it
							//increase the excluded words counter
							Statistics.increaseInt("excludedFilesCount");
							//increase the excluded words total size
							int fileSize = (int) file.length();
							Statistics.increaseInt("excludedFilesSize", fileSize);
						}
						//
						sendProgressDataToLogger(totalWords, currentWord);
					}
					if( !interruptionRequested ){
						//'folder' completed
						step4.setProperty("processedChars", step4.getProperty("processedChars")+","+folder);
						//store the step4 properties
						try {
							step4.store(new FileOutputStream(step4FlagFile), null);
						} catch (Exception e) { }
					}
				}
				//
				if( !interruptionRequested ){
					step4.setProperty("completed", "1");
				}
			}
			//close the executor
			executor.shutdown();
			while( !executor.isTerminated() ){
				try {
					executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) { }
			}
			//store the step4 properties
			try {
				step4.store(new FileOutputStream(step4FlagFile), null);
			} catch (Exception e) { }
			//
			Logger.log(this.getClass().getSimpleName(), "Step 4/4: Completed!\n");
		}else{
			Logger.log(this.getClass().getSimpleName(), "Step 4/4: Pre-Processed Data Loaded!\n");
		}
		//==FINALLY
		//close the file writer
		bfw.close();
		//close the knowledgebase
		knowledgeBase.close();
		//close the statistics
		Statistics.close();
		//close the command receiver
		commandReceiver.interrupt();
	}//buildKB


	//=> Protected/Private Methods

	@SuppressWarnings("unchecked")
	private Map<String,List<String>>[] readWordFile(File f, Map<String,Integer> wordOccur){
		Map<String,List<String>>[] result = null;
		//temporary structures
		String prefix, suffix;
		int totalSentences = 1;
		BufferedReader br;
		StringBuilder sb = new StringBuilder();
		//process the file
		Map<String,List<String>> intelliIOmap = new HashMap<String,List<String>>();
		try{
			String word = f.getName().replaceFirst("[.][^.]+$", ""); //<= remove the extension
			totalSentences = wordOccur.get(word);
			br = new BufferedReader( new FileReader( f ));
			//for each line
			String line = br.readLine();
			while( line != null ){
				//extract prefix and suffix
				prefix = line.substring(0, ConcurrentSentenceIDProvider.sentencePrefixLength);
				suffix = line.substring(ConcurrentSentenceIDProvider.sentencePrefixLength);
				//create the prefix path
				sb.setLength(0);
				for( int i = 0; i<ConcurrentSentenceIDProvider.sentencePrefixLength; i++ ){
					sb.append( File.separator+line.trim().charAt(i) );
				}
				prefix = sb.toString();
				//add the suffix to the prefix specific list
				List<String> list = intelliIOmap.get(prefix);
				if( list == null ){
					//create a new list
					list = new LinkedList<String>();
					intelliIOmap.put(prefix, list);
				}
				//add
				list.add(suffix);
				//get the next line
				line = br.readLine();
			}
			//close the reader
			br.close();
		}catch(Exception e){ }
		//compute the number of parts
		int parts = (int) Math.ceil( ((double)intelliIOmap.size()) / maxFilesPerThread );
		result = new Map[parts];
		Iterator<Map.Entry<String,List<String>>> it = intelliIOmap.entrySet().iterator();
		int k = 0;
		for( int i = 0; i<parts; i++ ){
			Map<String,List<String>> current = new HashMap<String,List<String>>();
			result[i] = current;
			//
			while( it.hasNext() && k<maxFilesPerThread ){
				Map.Entry<String,List<String>> entry = it.next();
				//
				current.put(entry.getKey(), entry.getValue());
				//
				k++;
			}
			//reset
			k = 0;
		}
		//
		Statistics.increaseInt("intelliOriginalIOcount", totalSentences);
		Statistics.increaseInt("intelliRealIOcount", intelliIOmap.size());
		//
		return result;
	}//readWordFile

	private File[] getSourceFiles( String sourceRegex ){
		if( sourceRegex != null ){
			int lastBar = sourceRegex.lastIndexOf( File.separatorChar );
			String path = sourceRegex.substring(0,lastBar+1);
			final String fileRegEx = sourceRegex.substring(lastBar+1).replace("*", "(\\w+)");

			File folder = new File(path);
			FilenameFilter fnf = new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					boolean flag = true;
					if( existsPartialData ){
						if( processedFiles.contains(name) ){
							//nullify the source file
							flag = false;
						}
					}
					return ( flag && name.matches(fileRegEx) );
				}
			};
			File[] files = folder.listFiles(fnf);
			return (files.length != 0)? files : null;
		}
		return null;
	}//getSourceFiles

	private void buildFolderStructure(){
		String folder = tempFolder;
		//create tempFolder
		new File( folder ).mkdirs();
		//create flags folder
		new File( folder+"flags" ).mkdir();
		//STEP1 =================================================================
		//create 'step1' folder
		folder = folder + "step1";
		new File( folder ).mkdir();
		//create words folders
		folder = folder + File.separator + "words";
		new File( folder ).mkdir();
		//create words first letter folders
		folder = folder + File.separator;
		for(char alphabet = 'a'; alphabet <= 'z'; alphabet++) {
			new File( folder + ((char)alphabet) ).mkdir();
		}
		//create sentenceIDs folders
		folder = tempFolder+"step1"+File.separator+"sentences";
		int l1 = folder.length();
		int l2 = -1, h = 0;
		List<String> ll = new LinkedList<String>();
		ll.add( folder );
		while( ll.size() != 0 ){
			String curr = ll.remove(0);
			l2 = curr.length();
			h = (l2 - l1)/2;
			//
			new File(curr).mkdir();
			//
			if( h < ConcurrentSentenceIDProvider.sentencePrefixLength-1 ){
				for( char c = 'A'; c <= 'Z'; c++ ){
					ll.add(curr+File.separator+c);
				}
			}
		}
		//STEP2 =================================================================
		folder = tempFolder;
		//create 'step2' folder
		folder = folder + "step2";
		new File( folder ).mkdir();
		//STEP3 =================================================================
		folder = tempFolder;
		//create 'step3' folder
		folder = folder + "step3";
		new File( folder ).mkdir();
		//create sentenceIDs folders
		folder = folder+File.separator;
		l1 = folder.length();
		l2 = -1;
		h = 0;
		ll = new LinkedList<String>();
		ll.add( folder );
		while( ll.size() != 0 ){
			String curr = ll.remove(0);
			l2 = curr.length();
			h = (l2 - l1)/2;
			//
			new File(curr).mkdir();
			//
			if( h < ConcurrentSentenceIDProvider.sentencePrefixLength-1 ){
				for( char c = 'A'; c <= 'Z'; c++ ){
					ll.add(curr+File.separator+c);
				}
			}
		}
	}//buildFolderStructure

	private void sendProgressDataToLogger( long totalLines, long currentLine ){
		long newTimeMillis = System.currentTimeMillis();
		int newPercentage = (int) (((double)currentLine/(double)(totalLines-1))*100);
		if( newPercentage != percentage ){
			String remainingTime = Formatter.formatTime( Statistics.getRemainingTime(percentage, newPercentage, timeMillis, newTimeMillis) );
			percentage = newPercentage;
			timeMillis = newTimeMillis;
			//
			Statistics.note("fileProgress", percentage);
			Statistics.note("fileRemainingTime", remainingTime);
		}
	}//sendProgressDataToLogger
	
	
	
	// Inner-Class
	
	private class CommandReceiver extends Thread{
		
		//==> Fields
		
		ServerSocket ss = null;
		
		
		//==> Methods
		
		@Override
		public void run(){
			int port = 14001; //<== default
			try{
				port = Integer.parseInt( Configuration.getConfiguration().getProperty("commandReceiverTCPport", "14001") );
			}catch(Exception e){}
			try {
				//
				ss = new ServerSocket( port );
				while( !isInterrupted() ){
					Socket s = ss.accept();
					//new client connected
					s.setSoTimeout(2000);
					try{
						BufferedReader br = new BufferedReader( new InputStreamReader( s.getInputStream() ) );
						BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( s.getOutputStream() ) );
						String line = br.readLine();
						if( line.trim().equals("interrupt") ){
							//interrupt request received
							KnowldgeBaseCompiler.this.interruptionRequested = true;
							Logger.log(this.getClass().getSimpleName(), "Interruption request received from "+s.toString());
						}
						//send a response
						bw.write( "OK!" );
						bw.flush();
						//
						s.shutdownInput();
						s.shutdownOutput();
					}catch(Exception e){}
					finally{
						try{
							s.close();
						}catch(Exception e){}
					}
				}
				//
			} catch (IOException e) {}finally{
				if( ss != null ){
					try {
						ss.close();
					} catch (IOException e) {}
				}
			}
		}//run
		
		@Override
		public void interrupt(){
			if( ss != null ){
				try {
					ss.close();
				} catch (IOException e) {}
			}
			//
			super.interrupt();
		}//interrupt
		
	}//CommandReceiver

}//KnowldgeBaseCompiler

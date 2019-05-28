package gov.nist.asbestos.simapi.simCommon;

import gov.nist.asbestos.simapi.tk.actors.ActorType;
import gov.nist.asbestos.simapi.tk.actors.TransactionType;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Each simulator has an on-disk presence that keeps track of its long
 * term status and a log of its input/output messages. This class
 * represents that on-disk presence.
 *
 * Simulators are created through the factory ActorSimulatorFactory and their
 * configurations are managed through ActorSimulatorConfig class.
 */

public class SimDb {
	SimId simId = null;    // ip is the simulator id
	private String event = null;
	private String eventDate;
	private File simDir = null;   // directory within simdb that represents this simulator
	private String actor = null;
	private String transaction = null;
	private File transactionDir = null;
	static private final Logger logger = Logger.getLogger(SimDb.class);
	private TestSession testSession = null;

	static final String MARKER = "MARKER";
	/**
	 * Base constructor Loads the simulator db directory
	 */
	public SimDb() {}
	/**
	 * open existing proxy
	 * @param simId
	 */
	public SimDb(SimId simId) {
		Objects.requireNonNull(simId);
		if (simId.getTestSession() == null || simId.getTestSession().getValue() == null)
			throw new RuntimeException("SimId not assigned to a TestSession - " + simId);
		File dbRoot = getSimDbFile(simId);
		this.simId = simId;
		validateSimId(simId);

		if (!dbRoot.exists())
			dbRoot.mkdirs();

		if (!dbRoot.isDirectory() || !dbRoot.canWrite())
			throw new RuntimeException("Simulator database location, [" + dbRoot.toString() + "] is not a directory or cannot be written to");

		String ipdir = simId.toString();
		simDir = new File(dbRoot.toString()  /*.getAbsolutePath()*/ + File.separatorChar + ipdir);
		if (!simDir.exists())
			throw new RuntimeException("Simulator " + simId + " does not exist (" + simDir + ")");

		simDir.mkdirs();

		if (!simDir.isDirectory())
			throw new RuntimeException("Cannot create content in Simulator database, creation of " + simDir.toString() + " failed");

		createSimSafetyFile();
	}

	public SimDb(SimId simId, ActorType actor, TransactionType transaction, boolean openToLastTransaction) {
		this(simId, actor.getShortName(), transaction.getShortName(), openToLastTransaction);
	}

	private SimDb(SimId simId, String actor, String transaction, boolean openToLastTransaction) {
		this(simId);
		Objects.requireNonNull(actor);
		this.actor = actor;
		this.transaction = transaction;

		if (transaction != null) {
			transactionDir = transactionDirectory(actor, transaction);
		} else
			return;

		if (openToLastTransaction) {
			openMostRecentEvent(actor, transaction)
		} else {
			Date date = new Date()
			SimpleDateFormat sdf = new SimpleDateFormat("yy_MM_dd_HH_mm_ss_SSS");
			eventDate = sdf.format(date);
			File eventDir = mkEventDir(eventDate);
			eventDir.mkdirs();
			try {
				Files.write(Paths.get(new File(eventDir, "date.txt").getPath()), eventDate.getBytes());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	SimDb mkSim(SimId simid, String actor) {
		validateSimId(simid);
			simid.forFhir();
			return mkfSim(simid);

//		File dbRoot = getSimDbFile(simid.getTestSession());
//		validateSimId(simid);
//		if (!dbRoot.exists())
//			dbRoot.mkdir();
//		if (!dbRoot.canWrite() || !dbRoot.isDirectory())
//			throw new RuntimeException("Simulator database location, " + dbRoot.toString() + " is not a directory or cannot be written to");
//
//		File simActorDir = new File(dbRoot.getAbsolutePath() + File.separatorChar + simid + File.separatorChar + actor);
//		simActorDir.mkdirs();
//		if (!simActorDir.exists())
//			throw new RuntimeException("Simulator " + simid + ", " + actor + " cannot be created");
//
//		return new SimDb(simid, actor, null, true);
	}

	public static SimId getFullSimId(SimId simId) {
		validateSimId(simId);
		SimId ssimId = new SimId(simId.getTestSession(), simId.getId());
		if (exists(ssimId)) {
			// soap based proxy
			SimDb simDb = new SimDb(ssimId);
			return internalSimIdBuilder(simDb.getSimDir(), simId.getTestSession());
		} else {
			ssimId = ssimId.forFhir();
			if (exists(ssimId)) {
				// FHIR based proxy
				SimDb simDb = new SimDb(ssimId);
				return internalSimIdBuilder(simDb.getSimDir(), simId.getTestSession());
			}
		}
		throw new RuntimeException("Simulator " + simId.toString() + " does not exist.");
	}


	/**
	 * All sims in this package are FHIR
	 * @return
	 */

	static File getSimDbFile(SimId simId) {
		validateSimId(simId);
		return gov.nist.asbestos.simapi.tk.installation.Installation.instance().simDbFile(simId.getTestSession());
	}

	static File getSimDbFile(TestSession testSession) {
		return Installation.instance().simDbFile(testSession);
	}

	public static boolean isFSim(SimId simId) {
		return true;
	}

	/**
	 * Does simulator exist?
	 * Checks for existence of simdb directory for passed id.
	 * @param simId id of simulator to check
	 * @return boolean true if a simulator directory for this id exists in the
	 * simdb directory, false otherwise.
	 */
	public static boolean exists(SimId simId) {
		return new File(getSimDbFile(simId), simId.toString()).exists();
	}

	private void createSimSafetyFile() {
		// add this for safety when deleting simulators -
		try {
			Files.write(Paths.get(simSafetyFile().getPath()), simId.toString().getBytes());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void openMostRecentEvent(ActorType actor, TransactionType transaction) {
		openMostRecentEvent(actor.getShortName(), transaction.getShortName());
	}

	private void openMostRecentEvent(String actor, String transaction) {
		this.actor = ActorType.findActor(actor).getShortName();
		this.transaction = TransactionType.find(transaction).getShortName();
		transactionDir = transactionDirectory(actor, transaction);
		File[] trans = transactionDir.listFiles();
		if (trans != null) {
			Arrays.sort(trans);
			String eventFullPath = (trans.length != 0) ? trans[trans.length-1].getPath() : null;
			if (eventFullPath != null) {
				File eventFile = new File(eventFullPath);
				event = eventFile.getName();
			}
		}
	}

	public static SimDb open(SimDbEvent event) {
		Objects.requireNonNull(event);
		Objects.requireNonNull(event.getSimId());
		Objects.requireNonNull(event.getActor());
		Objects.requireNonNull(event.getTrans());
		SimDb db = new SimDb(event.getSimId());
		db.transactionDir = db.transactionDirectory(event.getActor(), event.getTrans());
		db.event =  event.getEventId();
		db.actor = event.getActor();
		db.transaction = event.getTrans();
		return db;
	}

	private static void validateSimId(SimId simId)  {
		String badChars = " \t\n<>{}.";
		if (simId == null)
			throw new RuntimeException("Simulator ID is null");
		String id = simId.getId();
		if (id == null) {
			throw new RuntimeException("Simulator ID contains null ID");
		} else {
			for (int i = 0; i < badChars.length(); i++) {
				String c = String.valueOf(badChars.charAt(i));
				if (id.contains(c))
					throw new RuntimeException(String.format("Simulator ID contains bad character at position %d (%s)(%04x)", i, c, (int) c.charAt(0)));
			}
		}
		if (simId.getTestSession() == null || simId.getTestSession().getValue() == null)
			throw new RuntimeException("SimId not assigned to a TestSession - " + simId);
	}

	private File simSafetyFile() { return new File(simDir, "channelId.txt"); }

	public boolean isSim() {
		if (simDir == null) return false;
		return new File(simDir, "channelId.txt").exists();
	}
	private static boolean isSimDir(File dir) { return new File(dir, "channelId.txt").exists(); }


	public String getEventDate() {
		return eventDate;
	}


	public static SimDb createMarker(SimId simId) {
		return new SimDb(simId, MARKER, MARKER, false);
	}

	/**
	 * Events returned most recent first
	 * If no marker then return all events.
	 * (I think this method assumes there is only one actor type and actor type within the scope of a simulator because getAllEvents returns all events for all actors and all transactions.)
	 * @return
	 */
	public List<SimDbEvent> getEventsSinceMarker() {
		return getEventsSinceMarker(null, null);
	}

	public List<SimDbEvent> getEventsSinceMarker(String actor, String tran) {
		List<SimDbEvent> events = getAllEvents(actor, tran);
		Map<String, SimDbEvent> eventMap = events.stream()
				.collect(Collectors.toMap(SimDbEvent::getEventId, Function.identity()));
		List<String> ordered = new ArrayList<>(eventMap.keySet());
		ordered.sort(Comparator.reverseOrder());
		List<SimDbEvent> selected = new ArrayList<>();
		for (String name : ordered) {
			SimDbEvent event = eventMap.get(name);
			if (MARKER.equals(event.getActor()))
				selected.add(event);
		}
		return selected;
	}

	/**
	 * Used by simproxy to get outbound proxy half of proxy to have same event id (time stamp)
	 * @param otherSimDb
	 */
	public void mirrorEvent(SimDb otherSimDb, String actor, String transaction) {
		this.actor = actor;
		this.transaction = transaction;
		transactionDir = transactionDirectory(actor, transaction);
		String event = otherSimDb.event;
		eventDate = otherSimDb.getEventDate();
		File eventDir = mkEventDir(event);
		eventDir.mkdirs();
		Path path = new File(eventDir, "date.txt").toPath();
		try {
			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				writer.write(eventDate);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public File transactionDirectory(String actor, String transaction) {
		Objects.requireNonNull(actor);
		Objects.requireNonNull(transaction);
		String transdir = new File(new File(simDir, actor), transaction).getPath();
		File dir = new File(transdir);
		dir.mkdirs();
		if (!dir.isDirectory())
			throw new RuntimeException("Cannot create content in Simulator database, creation of " + transactionDir + " failed");
		return dir;
	}

	 File mkEvent(String transaction) {
		Date date = new Date();
		File eventDir = mkEventDir(date);
		eventDir.mkdirs();
		return eventDir;
	}

	private File mkEventDir(Date date) {
		String eventBase = Installation.asFilenameBase(date);
		return mkEventDir(eventBase);
	}

	private File mkEventDir(String eventBase) {
		int incr = 0;
		while (true) {
			event = eventBase;
			if (incr != 0)
				event += '_' + incr;    // make unique
			File eventDir = getEventDir();  // from event
			if (eventDir.exists()) {
				// must be fresh new dir - try again
				incr++;
			}
			else
				break;
		}
		return getEventDir();
	}

	public String getEvent() { return event; }
	public void setEvent(String _event) { event = _event; }
	public void setEvent(SimDbEvent event) {
		this.actor = event.getActor();
		this.transaction = event.getTrans();
		configureTransactionDir();
		this.event = event.getEventId();
	}

	 File getEventDir() {
		return new File(transactionDir, event);
	}

	public void setClientIpAddess(String clientIpAddress)  {
		if (clientIpAddress != null) {
			Path path = new File(getEventDir(), "ip.txt").toPath();
			try {
				try (BufferedWriter writer = Files.newBufferedWriter(path)) {
					writer.write(clientIpAddress);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public String getClientIpAddress() {
		File eventDir = getEventDir();
		if (eventDir != null && eventDir.isDirectory()) {
			try {
				return new String(Files.readAllBytes(new File(eventDir, "ip.txt").toPath()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private void configureTransactionDir() {
		String transdir = new File(new File(simDir, actor), transaction).getPath();
		transactionDir = new File(transdir);
		transactionDir.mkdirs();
		if (!transactionDir.isDirectory())
			throw new RuntimeException("Cannot create content in Simulator database, creation of " + transactionDir + " failed");
	}

	// actor, actor, and event must be filled in
	private String retrieveEventDate() {
		if (transactionDir == null || event == null) return null;
		File eventDir = new File(transactionDir, event);
		try {
			eventDate = new String(Files.readAllBytes(new File(eventDir, "date.txt").toPath()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return eventDate;
	}

	/**
	 * Delete simulator
	 */
	public void delete() {
		if (isSim()) {
			if (simId != null) {
				delete(simDir);
			}
		}
	}

	public List<String> getActorsForSimulator() {
		List<String> actors = new ArrayList<>();
		File[] files = simDir.listFiles();
		for (File file : files) {
			if (file.isDirectory())
				actors.add(file.getName());
		}
		return actors;
	}

//	static  Date getNewExpiration(@SuppressWarnings("rawtypes") Class controllingClass)   {
//		// establish expiration for newly touched cache elements
//		Date now = new Date();
//		Calendar newExpiration = Calendar.getInstance();
//		newExpiration.setTime(now);
//
//		String dayOffset = null
//		if (dayOffset == null) {
//			dayOffset = "1";
//		}
//		newExpiration.add(Calendar.DAY_OF_MONTH, Integer.parseInt(dayOffset));
//		return newExpiration.getTime();
//	}

	public void deleteAllSims(TestSession testSession) {
		List<SimId> allSimIds = getAllSimIds(testSession);
		for (SimId simId : allSimIds) {
			SimDb db = new SimDb(simId);
			db.delete();
		}
	}

	public static  void deleteSims(List<SimId> simIds) {
		for (SimId simId : simIds) {
			try {
				SimDb db = new SimDb(simId);
				db.delete();
			} catch (Throwable e) { } // ignore
		}
	}

	static private SimId internalSimIdBuilder(File simDefDir, TestSession testSession) {
		SimId simId = SimIdFactory.simIdBuilder(simDefDir.getName());  //new SimId(testSession, simDefDir.name)
		if (isFSim(simId)) simId.forFhir();
		try {
			simId.setActorType(new SimDb(simId).getSimulatorType());
		} catch (Exception e) {

		}
		return simId;
	}

	static SimId simIdBuilder(String rawId) {
		SimIdFactory.simIdBuilder(rawId);
	}

	// is this proxy valid - does it have the necessary parts
	public static boolean isValid(SimId simId) {
		File d = getSimDbFile(simId)
		File f = new File(d, simId.toString())
		if (! new File(f, "channelId.txt").exists())
			return false
		if (! new File(f, "simctl.json").exists())
			return false
		return true;
	}

	private static List<SimId> getSimIdsInTestSession(TestSession testSession) {
		List<SimId> ids = new ArrayList<>();
		File[] sims = getSimDbFile(testSession).listFiles();
		if (sims == null)
			return ids;
		for (File sim : sims) {
			if (isSimDir(sim)) {
				ids.add(internalSimIdBuilder(sim, testSession));
			}
		}
		return ids;
	}

	public static List<SimId> getAllSimIds(TestSession testSession) {
		List<SimId> testSessionSimIds = getSimIdsInTestSession(testSession);
		if (testSession == TestSession.DEFAULT_TEST_SESSION)
			return testSessionSimIds;
		List<SimId> defaultSimIds = getSimIdsInTestSession(TestSession.DEFAULT_TEST_SESSION);

		Set<SimId> ids = new HashSet<>();
		ids.addAll(testSessionSimIds);
		ids.addAll(defaultSimIds);
		return new ArrayList<>(ids);
	}

	/**
	 * should always use SimId - carries more information
	 * @return
	 */
	public static List<String> getAllSimNames(TestSession testSession) {
		return getAllSimIds(testSession).stream()
				.map(SimId::getId)
				.collect(Collectors.toList());
	}

	/**
	 * Get a simulator.
	 * @return simulator if it exists or null
	 *
	 */
	public SimulatorConfig getSimulator(SimId simId)  {
		SimulatorConfig config = null;
		boolean okIfNotExist = true;
		int retry = 3;
		// Sometimes loadSimulator returns Null even though there is a valid simulator
		while (config == null && retry-->0) {
			try {
				config = GenericSimulatorFactory.loadSimulator(simId, okIfNotExist);
			} catch (Exception ex) {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				logger.info("LoadSimulator retrying attempt..." + retry);
			}
		}

//		if (!okIfNotExist && config == null)
//			throw new RuntimeException("Null config for " + simId.toString() + " even after retry attempts.");

		return config;
	}

	public  File getSimulatorControlFile() {
		return new File(simDir.toString() + File.separatorChar + "simctl.json");
	}

	static  String getTransactionDirName(TransactionType tt)  {
		return tt.getShortName();
	}

	 File getTransactionDir(TransactionType tt) {
		String trans = getTransactionDirName(tt);
		return new File(new File(simDir, actor), trans);
	}

	public List<String> getTransactionNames(String actorType) {
		File transDir = new File(simDir.toString() + File.separator + actorType);
		List<String> names = new ArrayList<String>();

			File[] files = transDir.listFiles();
			if (files == null)
				return new ArrayList<>();
			for (File f : files) {
				if (f.isDirectory())
					names.add(f.getName());
			}

		return names;
	}

	public  String getTransaction() { return transaction; }
	public  String getActor() { return actor; }
	public  SimId getSimId() { return simId; }

	static String stripFileType(String filename, String filetype) {
		int dot = filename.lastIndexOf("." + filetype);
		if (dot == -1) return filename;
		return filename.substring(0, dot);
	}

	public ActorType getSimulatorActorType() {
		SimulatorConfig config = AbstractActorFactory.getSimConfig(simId);
		ActorType.findActor(config.getActorType());
	}

	public static List<SimId> getSimulatorIdsforActorType(ActorType actorType, TestSession testSession)  {
		List<SimId> allSimIds = getAllSimIds(testSession);
		List<SimId> simIdsOfType = new ArrayList<>();
		for (SimId simId : allSimIds) {
			if (actorType.equals(getSimulatorActorType(simId)))
				simIdsOfType.add(simId);
		}

		return simIdsOfType;
	}

	public static  ActorType getSimulatorActorType(SimId simId)  {
		return new SimDb(simId).getSimulatorActorType();
	}

	public  List<String> getTransactionsForSimulator() {
		List<String> trans = new ArrayList<>();

		File[] actors = simDir.listFiles();
		if (actors != null) {
			for (File actor : actors) {
				if (!actor.isDirectory())
					continue;
				File[] tranx = actor.listFiles();
				if (tranx != null) {
					for (File tr : tranx) {
						if (tr != null) {
							if (!tr.isDirectory())
								continue;
							trans.add(tr.getName());
						}
					}
				}
			}
		}

		return trans;
	}

	public  String getSimulatorType() {
		SimulatorConfig config = AbstractActorFactory.getSimConfig(simId);
		return config.getActorType();
	}

		static private String oidToFilename(String oid) {
		return oid.replaceAll("\\.", "_");
	}

	static String filenameToOid(String filename) {
		return filename.replaceAll("_", ".");
	}

	 String getFileNameBase() {
		return event;
	}

	 void setFileNameBase(String base) {
		event = base;
	}

	 File getSimDir() {
		return getIpDir();
	}

	 File getIpDir() {
		return simDir;
	}

	public  List<TransactionInstance> getTransInstances(String ignored_actor, String trans) {
		String event_save = event;
		File transDir_save = transactionDir;
		List<String> names = new ArrayList<String>();
		List<TransactionInstance> transList = new ArrayList<>();

		for (File actor : listFiles(simDir)) {
			if (!actor.isDirectory())
				continue;
			for (File tr : listFiles(actor)) {
				if (!tr.isDirectory())
					continue;
				String name = tr.getName();
				if (trans != null && !name.equals(trans) && !trans.equals(("All")))
					continue;
				for (File inst : listFiles(tr)) {
					if (!inst.isDirectory())
						continue;
					names.add(inst.getName() + " " + name);

					TransactionInstance t = buildTransactionInstance(actor, inst, name);

					//logger.debug("Found " + t);
					if (!t.isPif)
						transList.add(t);
				}
			}
		}

		transList.sort((TransactionInstance t1, TransactionInstance t2) -> t2.getMessageId().compareTo(t1.getMessageId()));

		event = event_save;
		transactionDir = transDir_save;
		return transList;
	}

	private static List<File> listFiles(File dir) {
		File[] files = dir.listFiles();
		if (files == null)
			return new ArrayList<>();
		return Arrays.asList(files);
	}

	private File getActorDir(String actor) {
		return new File(simDir, actor);
	}

	public TransactionInstance buildTransactionInstance(String actor, String messageId, String trans) {
		return buildTransactionInstance(getActorDir(actor), new File(messageId) , trans);
	}

	// This messes with transactionDir which must be saved before and restored afterwards
	public TransactionInstance buildTransactionInstance(File actor, File inst, String name) {
		TransactionInstance t = new TransactionInstance();
		t.simId = simId.toString();
		t.actorType = gov.nist.asbestos.simapi.tk.actors.ActorType.findActor(actor.getName());
		t.messageId = inst.getName();
		t.trans = name;
		transactionDir = new File(actor, name);
		//logger.debug("actor dir is " + transactionDir);
		event = t.messageId;
		String date = null;
			date = retrieveEventDate();
		t.labelInterpretedAsDate = date;
		t.nameInterpretedAsTransactionType = TransactionType.find(t.trans);

		Path ipAddrFile = new File(inst, "ip.txt").toPath();
		try {
			String ipAddr = new String(Files.readAllBytes(ipAddrFile));
			if (!ipAddr.equals(""))
				t.ipAddress = ipAddr;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return t;
	}

	 public List<File> getTransInstanceFiles(String actor, String trans) {
		Objects.requireNonNull(actor);
		Objects.requireNonNull(trans);
		return listFiles(new File(new File(simDir, actor), trans));
	}

	private File getDBFilePrefix(String event) {
		Objects.requireNonNull(simDir);
		Objects.requireNonNull(actor);
		Objects.requireNonNull(transaction);
		Objects.requireNonNull(event);
		File f = new File(new File(new File(simDir, actor), transaction), event);
		f.mkdirs();
		return f;
	}

	 public File getResponseBodyFile() {
		return new File(getDBFilePrefix(event), "response_body.txt");
	}

	public void putResponseBody(String content) {
		Path path = getResponseBodyFile().toPath();
		try {
			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				writer.write(content);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void putResponseBody(byte[] content) {
		try {
			try (FileOutputStream stream = new FileOutputStream(getResponseBodyFile())) {
				stream.write(content);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getResponseBody() {
		Path path = getResponseBodyFile().toPath();
		try {
			return new String(Files.readAllBytes(path));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean responseBodyExists() {
		return getResponseBodyFile().exists();
	}

	 File getResponseHdrFile() {
		return new File(getDBFilePrefix(event), RESPONSE_HEADER_FILE);
	}

	static final String REQUEST_HEADER_FILE = 'request_hdr.txt'
	static final String REQUEST_BODY_TXT_FILE = 'request_body.txt'
	static final String REQUEST_BODY_BIN_FILE = 'request_body.bin'
	static final String RESPONSE_HEADER_FILE = 'response_hdr.txt'
	static final String RESPONSE_BODY_TXT_FILE = 'response_body.txt'
	static final String REQUEST_URI_FILE = 'request_uri.txt'

	private File getRequestMsgHdrFile(String filenamebase) {
		assert filenamebase
		return new File(getDBFilePrefix(filenamebase), REQUEST_HEADER_FILE);
	}

	File getRequestURIFile(String filenamebase) {
		assert filenamebase
		return new File(getDBFilePrefix(filenamebase), REQUEST_URI_FILE)
	}

	private File getRequestMsgBodyFile(String filenamebase) {
		assert filenamebase
		return new File(getDBFilePrefix(filenamebase), REQUEST_BODY_BIN_FILE);
	}

	private File getAlternateRequestMsgBodyFile(String filenamebase) {
		assert filenamebase
		return new File(getDBFilePrefix(filenamebase), REQUEST_BODY_TXT_FILE);
	}

	private File getResponseMsgHdrFile(String filenamebase) {
		assert filenamebase
		return new File(getDBFilePrefix(filenamebase), RESPONSE_HEADER_FILE);
	}

	private File getResponseMsgBodyFile(String filenamebase) {
		assert filenamebase
		return new File(getDBFilePrefix(filenamebase), RESPONSE_BODY_TXT_FILE);
	}

	 String getRequestMessageHeader() {
		return getRequestMessageHeader(event);
	}

	 String getRequestMessageHeader(String filenamebase) {
		File f = getRequestMsgHdrFile(filenamebase);
		 assert f.exists() : "SimDb: Simulator Database file " + f.toString() + " does not exist"
		return f.text
	}

	 String getResponseMessageHeader() {
		return getResponseMessageHeader(event);
	}

	 String getResponseMessageHeader(String filenamebase) {
		File f = getResponseMsgHdrFile(filenamebase);
		 assert f.exists() : "SimDb: Simulator Database file " + f.toString() + " does not exist"
		return f.text
	}

	 byte[] getRequestMessageBody() {
		return getRequestMessageBody(event);
	}

	 byte[] getRequestMessageBody(String filenamebase)  {
		File f = getRequestMsgBodyFile(filenamebase);
		 assert f.exists() : "SimDB: ${f} does not exist"
		 return f.bytes
	}

	 byte[] getResponseMessageBody()  {
		return getResponseMessageBody(event);
	}

	 byte[] getResponseMessageBody(String filenamebase)  {
		File f = getResponseMsgBodyFile(filenamebase);
		 assert f.exists() : "SimDB: ${f} does not exist"
		 return f.bytes
	}

	 File getLogFile() {
		return new File(getDBFilePrefix(event), "log.txt");
	}

	 void delete(String fileNameBase)  {
		File f = getDBFilePrefix(fileNameBase);
		delete(f);
	}

	 void delete(File f) {
		 f.delete()
	}

	void rename(String fileNameBase, String newFileNameBase)  {

		File from = getDBFilePrefix(fileNameBase);
		File to = getDBFilePrefix(newFileNameBase);
		boolean stat = from.renameTo(to);
		assert stat : "Rename failed"
	}

	// name of proxy directory is the name we want
	// make sure internals are up to date with it
	void updateSimConfiguration() {
		createSimSafetyFile()  // actually update
	}

	private File findEventDir(String trans, String event) {
		for (File actor : simDir.listFiles()) {
			if (!actor.isDirectory())
				continue;
			File eventDir = new File(new File(actor, trans), event)
			if (eventDir.exists() && eventDir.isDirectory())
				return eventDir;
		}
		return null;
	}

	List<SimDbEvent> getAllEvents() {
		getAllEvents(null, null);
	}

	/**
	 *
	 * @param actor Optional.
	 * @param tran Optional.
	 * @return
	 */
	List<SimDbEvent> getAllEvents(String actor, String tran) {
		List<SimDbEvent> eventDirs = []
		for (File actorDir : simDir.listFiles()) {
			if (!actorDir.isDirectory()) continue
			if (actor?actorDir.getName().equals(actor):true) {
				for (File transDir : actorDir.listFiles()) {
					if (!transDir.isDirectory()) continue
                    if ((tran?transDir.getName().equals(tran):true)) {
						for (File eventDir : transDir.listFiles()) {
							eventDirs << new SimDbEvent(simId, actorDir.name, transDir.name, eventDir.name)
						}
					}
				}
			}
		}
		return eventDirs
	}


	 File getTransactionEvent(String simid, String actor, String trans, String event) {
		return new File(new File(new File(simDir, actor), trans), event)
	}

	 File getRequestHeaderFile(SimId simid, String actor, String trans, String event) {
		File dir = findEventDir(trans, event);
		if (dir == null)
			return null;
		return new File(dir, "request_hdr.txt");
	}

	 File getResponseHeaderFile(SimId simid, String actor, String trans, String event) {
		File dir = findEventDir(trans, event);
		if (dir == null)
			return null;
		return new File(dir, "response_hdr.txt");
	}

	 File getRequestBodyFile(SimId simid, String actor, String trans, String event) {
		File dir = findEventDir(trans, event);
		if (dir == null)
			return null;
		return new File(dir, "request_body.bin");
	}

	 File getResponseBodyFile(SimId simid, String actor, String trans, String event) {
		File dir = findEventDir(trans, event);
		if (dir == null)
			return null;
		return new File(dir, "response_body.txt");
	}

	 File getLogFile(SimId simid, String actor, String trans, String event) {
		File dir = findEventDir(trans, event);
		if (dir == null)
			return null;
		return new File(dir, "log.txt");
	}

	 List<String> getRegistryIds(String simid, String actor, String trans, String event) {
		List<String> ids = new ArrayList<String>();

		File dir = getTransactionEvent(simid, actor, trans, event);
		File registry = new File(dir.toString() + File.separator + "Registry");

		if (registry.exists()) {
			for (File f : registry.listFiles()) {
				String filename = f.getName();
				int dotI = filename.indexOf('.');
				if (dotI != -1) {
					String name = filename.substring(0, dotI);
					ids.add(name);
				}
			}
		}
		return ids;
	}

	File getRequestURIFile() {
		assert event
		return getRequestURIFile(event)
	}

	 File getRequestHeaderFile() {
		assert event
		return getRequestMsgHdrFile(event);
	}

	 File getRequestBodyFile() {
		assert event
		return getRequestMsgBodyFile(event);
	}

	private File getAlternateRequestBodyFile() {
		assert event
		return getAlternateRequestMsgBodyFile(event);
	}

	void putRequestURI(String uri) {
		File f = getRequestURIFile()
		OutputStream out = new FileOutputStream(f)
		try {
			out.write(uri.bytes);
		} finally {
			out.close();
		}
	}

	 void putRequestHeaderFile(byte[] bytes) {
		File f = getRequestHeaderFile();
		 f.bytes = bytes
	}

	 void putRequestBodyFile(byte[] bytes) {
		 getRequestBodyFile().bytes = bytes
		 getAlternateRequestBodyFile().text = new String(bytes)
	}

	void putResponseHeaderFile(byte[] bytes) {
		getResponseHdrFile().bytes = bytes
	}

	/**************************************************************************
	 *
	 * FHIR Support
	 *
	 **************************************************************************/

	static final String BASE_TYPE = 'fhir'
	final static String ANY_TRANSACTION = 'any'

	/**
	 * Store a Resource in a proxy
	 * @param resourceType  - index type (Patient...)
	 * @param resourceContents - JSON for index
	 * @return file where resource stored is ResDb
	 */
	File storeNewResource(String resourceType, String resourceContents, String id) {

		File resourceTypeDir = new File(getEventDir(), resourceType)

		resourceTypeDir.mkdirs()
		File file = new File(resourceTypeDir, "${id}.json")
		file.text = resourceContents
		return file
	}

	/**
	 * Return base dir of SimDb storage for FHIR resources (all FHIR simulators)
	 * This allows the inheritance to SimDb to work - SimDb actually manages
	 * both the SOAP simulators and the FHIR simulators.  This method controls
	 * which.
	 * @return
	 */

	static File getResDbFile(TestSession testSession) {
		return gov.nist.asbestos.simapi.tk.installation.Installation.instance().fhirSimDbFile(testSession)
	}


	/**
	 * Base location of FHIR simulator
	 * @param simId - which simulator
	 * @return
	 */
	static File getSimBase(SimId simId) {
		assert simId?.testSession?.value
		return new File(getResDbFile(simId.testSession), simId.toString())
	}


	/**
	 * delete FHIR proxy
	 * @param simId
	 * @return
	 */
	static boolean fdelete(SimId simId) {
		if (!fexists(simId)) return false
		getSimBase(simId).delete()
		return true
	}

	/**
	 * Does fhir simulator exist?
	 * @param simId
	 * @return
	 */
	static  boolean fexists(SimId simId) {
		return getSimBase(simId).exists()
	}

	static SimDb mkfSim(SimId simid)  {
		assert simid?.testSession?.value
		return mkfSimi(getResDbFile(simid.testSession), simid, BASE_TYPE, true)
	}

	private static SimDb mkfSimi(File dbRoot, SimId simid, String actor, boolean openToLastEvent)  {
		simid.forFhir()
		validateSimId(simid);
		if (!dbRoot.exists())
			dbRoot.mkdirs();
		assert dbRoot.canWrite() && dbRoot.isDirectory() : "Resource Simulator database location, " + dbRoot.toString() + " is not a directory or cannot be written to"

		File simActorDir = new File(dbRoot.getAbsolutePath() + File.separatorChar + simid + File.separatorChar + actor);
		simActorDir.mkdirs();
		assert simActorDir.exists() : "FHIR Simulator " + simid + ", " + actor + " cannot be created"

		return new SimDb(simid, BASE_TYPE, null, openToLastEvent);
	}

	void setActor(String actor) {
		this.actor = actor
	}

	void setTransaction(String transaction) {
		this.transaction = transaction
	}

	TestSession getTestSession() {
		if (!testSession)
			testSession = simId.testSession
		return testSession
	}

	@Override
	String toString() {
		simId.toString()
	}

}

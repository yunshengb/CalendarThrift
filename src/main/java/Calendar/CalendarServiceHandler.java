package  calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import thrift.*;
import nlp.TextProcessor;

public class CalendarServiceHandler implements LucidaService.Iface {
	/** Application name. */
	private static final String APPLICATION_NAME =
			"Google Calendar Thrift";

	/** Directory to store user credentials for this application. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(
			System.getProperty("user.home"), ".credentials/calendar-java.json");

	/** Global instance of the {@link FileDataStoreFactory}. */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY =
			JacksonFactory.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	/** Global instance of the scopes required by this program.
	 *
	 * If modifying these scopes, delete your previously saved credentials
	 * at ~/.credentials/calendar-java.json
	 */
	private static final List<String> SCOPES =
			Arrays.asList(CalendarScopes.CALENDAR_READONLY);
	
	private static TextProcessor p;

	static {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
			p = new TextProcessor();
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Creates an authorized Credential object.
	 * @return an authorized Credential object.
	 * @throws IOException
	 */
	public static Credential authorize() throws IOException {
		// Load client secrets.
		InputStream in = new FileInputStream("/Users/yba/Documents/U/Sirius/Calendar/src/main/resources/client_secret.json");
				//alendar.class.getResourceAsStream("../../resources/client_secret.json");
		System.out.println(in);
//		if (in == null) {
//			System.exit(1);
//		}
		GoogleClientSecrets clientSecrets =
				GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow =
				new GoogleAuthorizationCodeFlow.Builder(
						HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
		.setDataStoreFactory(DATA_STORE_FACTORY)
		.setAccessType("offline")
		.build();
		Credential credential = new AuthorizationCodeInstalledApp(
				flow, new LocalServerReceiver()).authorize("user");
		System.out.println(
				"Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
		return credential;
	}

	/**
	 * Builds and returns an authorized Calendar client service.
	 * @return an authorized Calendar client service
	 * @throws IOException
	 */
	public static com.google.api.services.calendar.Calendar
	getCalendarService() throws IOException {
		Credential credential = authorize();
		return new com.google.api.services.calendar.Calendar.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, credential)
		.setApplicationName(APPLICATION_NAME)
		.build();
	}
	
	List<Event> getEventsHelper(com.google.api.services.calendar.Calendar service,
			String[] parsed_time) throws IOException, ParseException {
		System.out.println(parsed_time[0]);
		System.out.println(parsed_time[1]);
		if (parsed_time[0] == null) {
			Events events = service.events().list("primary")
					.setMaxResults(100) // What if the user wants too many events?
					.setTimeMax(new DateTime(new SimpleDateFormat("yyyy-MM-dd hh:mm").parse(parsed_time[1])))
					.setOrderBy("startTime")
					.setSingleEvents(true)
					.execute();
			return events.getItems();
		} else if (parsed_time[1] == null) {
			Events events = service.events().list("primary")
					.setMaxResults(100) // What if the user wants too many events?
					.setTimeMin(new DateTime(new SimpleDateFormat("yyyy-MM-dd hh:mm").parse(parsed_time[0])))
					.setOrderBy("startTime")
					.setSingleEvents(true)
					.execute();
			return events.getItems();
		} else if (parsed_time[0].equals("upcoming 10 events")) {
			Events events = service.events().list("primary")
					.setMaxResults(10) 
					.setOrderBy("startTime")
					.setSingleEvents(true)
					.execute();
			return events.getItems();
		} else {
			Events events = service.events().list("primary")
					.setMaxResults(100) // What if the user wants too many events?
					.setTimeMin(new DateTime(new SimpleDateFormat("yyyy-MM-dd hh:mm").parse(parsed_time[0])))
					.setTimeMax(new DateTime(new SimpleDateFormat("yyyy-MM-dd hh:mm").parse(parsed_time[1])))
					.setOrderBy("startTime")
					.setSingleEvents(true)
					.execute();
			return events.getItems();
		}
	}

	/** 
	 * Returns the upcoming events.
	 */
	public List<String> getEvents(String[] parsed_time) {
		List<String> rtn = new ArrayList<String>();
		try {
			// Build a new authorized API client service.
			// Note: Do not confuse this class with the
			//   com.google.api.services.calendar.model.Calendar class.
			com.google.api.services.calendar.Calendar service =
					getCalendarService();

			List<Event> items = getEventsHelper(service, parsed_time);
			if (items.size() == 0) {
				System.out.println("No upcoming events found.");
			} else {
				System.out.println("Upcoming events");
				for (Event event : items) {
					DateTime start = event.getStart().getDateTime();
					if (start == null) {
						start = event.getStart().getDate();
					}
					String s = event.getSummary() + "(" + start + ")";
					rtn.add(s);
					System.out.println(s);
				}
			}
			return rtn;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void create(String LUCID, QuerySpec spec) {
		System.out.println("Create");
	}

	public void learn(String LUCID, QuerySpec knowledge) {
		System.out.println("Learn");
	}

	public String infer(String LUCID, QuerySpec query) {
		if (query.content.isEmpty() ||
				!query.content.get(0).type.toLowerCase().equals("calendar")) {
			throw new IllegalArgumentException();
		}
		String rtn = "";
		for (String s : getEvents(p.parse(query.content.get(0).data.get(0)))) {
			rtn += s + "\n";
		}
		return rtn;
	}

	public void ping() {
		System.out.println("Ping");
	}


}

//Java packages
import java.util.List;
import java.util.ArrayList;

//Thrift java libraries 
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

//Generated code
import edu.umich.clarity.thrift.CalendarService;

/** 
* A Calendar Client that get the upcoming events from Calendar Server and prints the results.
*/
public class CalendarClient {
	public static void main(String [] args) {
		// Collect the port number.
		int port = 9091;

		if (args.length == 1) {
			port = Integer.parseInt(args[0]);
		} else {
			System.out.println("Using default port for Calendar Client: " + port);
		}
	 
		// Initialize thrift objects.
		// TTransport transport = new TSocket("clarity08.eecs.umich.edu", port);
		TTransport transport = new TSocket("localhost", port);
		TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
		CalendarService.Client client = new CalendarService.Client(protocol);
		try {
			// Talk to the Calendar server.
			transport.open();
			System.out.println("///// Connecting to Calendar... /////");
			List<String> results = client.getEvents();
			System.out.println("///// Results: /////");
			for (String result : results) {
				System.out.println(result);
			}
			transport.close();
		} catch (TException e) {
			e.printStackTrace();
		}
		
		return;
	}
}

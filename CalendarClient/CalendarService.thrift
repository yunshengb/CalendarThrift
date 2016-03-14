namespace java edu.umich.clarity.thrift

service CalendarService {
	# CalendarService <--> client API
	list<string> getEvents(),

	# simple function to test connections
	void ping()
}

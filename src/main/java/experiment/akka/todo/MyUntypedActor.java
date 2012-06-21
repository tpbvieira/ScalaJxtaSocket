package experiment.akka.todo;

import net.jxta.endpoint.Message;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MyUntypedActor extends UntypedActor {
	
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);	
	
	public void onReceive(Object message) throws Exception {
		if (message instanceof String)
			System.out.println("Received String message: " + (String)message);
		else if (message instanceof Message)
			System.out.println("Received JXTA message: " + (Message)message);
		else
			unhandled(message);
	}
}

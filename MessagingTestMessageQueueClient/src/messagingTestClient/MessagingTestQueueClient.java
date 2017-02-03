package messagingTestClient;

import java.util.Hashtable;
import javax.jms.JMSException;
import javax.jms.Message;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This is the client that invokes the StackStarterMDB. See StackStarterMDB for usage instructions.
 *
 * @author Angelo.Fuchs
 */
public class MessagingTestQueueClient {

	// ALTERNATE THE AMOUNT IN HERE!
	public static void main(String[] args) throws Exception {
		new MessagingTestQueueClient().sendMessageWithUser("devel", "password1!", 55);
	}

	private void sendMessageWithUser(String user, String password, int amount) throws Exception {
		Hashtable environment = getEnv(user, password);
		try {
			InitialContext context = new InitialContext(environment);
			QueueConnection conn = openConnection(context, user, password);
			QueueSession session = conn.createQueueSession(false,
				QueueSession.AUTO_ACKNOWLEDGE);
			Queue faEndpoint = (Queue) context.lookup("jms/queue/TestStackStarterQueue");
			QueueSender send = session.createSender(faEndpoint);

			Message message = session.createMessage();
			message.setIntProperty("amount", amount);
			send.send(message);
			send.close();
			session.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Sending failed for user: " + user + " password: " + password);
			e.printStackTrace();
		}
	}

	private QueueConnection openConnection(InitialContext context, String user, String password) throws NamingException, JMSException {
		QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
		QueueConnection conn = qcf.createQueueConnection(user, password);
		return conn;
	}

	private Hashtable getEnv(String user, String password) throws Exception {
		Hashtable environment = new Hashtable();
		environment.put(InitialContext.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
		environment.put(InitialContext.PROVIDER_URL, "remote://localhost:4447");
		if (user != null) {
			environment.put(InitialContext.SECURITY_PRINCIPAL, user);
		}
		if (password != null) {
			environment.put(InitialContext.SECURITY_CREDENTIALS, password);
		}
		return environment;
	}
}

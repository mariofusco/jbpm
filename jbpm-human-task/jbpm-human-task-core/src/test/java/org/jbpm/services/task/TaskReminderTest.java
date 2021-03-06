package org.jbpm.services.task;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jbpm.services.task.impl.factories.TaskFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.internal.task.api.InternalTaskService;
import org.kie.internal.task.api.model.InternalTask;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class TaskReminderTest extends HumanTaskServicesBaseTest {
	
	private PoolingDataSource pds;
	private EntityManagerFactory emf;
	private Wiser wiser;
	@Before
	public void setup() {
		Properties conf = new Properties();
        conf.setProperty("mail.smtp.host", "localhost");
        conf.setProperty("mail.smtp.port", "2345");
        conf.setProperty("mail.from", "from@domain.com");
        conf.setProperty("mail.replyto", "replyTo@domain.com");
        
        wiser = new Wiser();
        wiser.setHostname(conf.getProperty("mail.smtp.host"));
        wiser.setPort(Integer.parseInt(conf.getProperty("mail.smtp.port")));        
        wiser.start();
        try {
        	Thread.sleep(1000);
        } catch (Throwable t) {
        	// Do nothing
        }
        
		pds = setupPoolingDataSource();
		emf = Persistence.createEntityManagerFactory( "org.jbpm.services.task" );
		this.taskService = (InternalTaskService) HumanTaskServiceFactory.newTaskServiceConfigurator()
												.entityManagerFactory(emf)
												.getTaskService();
	}
	
	@After
	public void clean() {
		if (wiser != null) {
            wiser.stop();
            try {
            	Thread.sleep(1000);
            } catch (Throwable t) {
            	// Do nothing
            }
        }
		super.tearDown();
		if (emf != null) {
			emf.close();
		}
		if (pds != null) {
			pds.close();
		}
	}
	
	 @Test
	public void testTaskReminderWithoutNotification() throws Exception {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("now", new Date());

		Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.ReminderWithoutNotification));
		InternalTask task = (InternalTask) TaskFactory.evalTask(reader, vars);
		System.out.println("testTaskReminderWithoutNotification " + task.getTaskData().getStatus());
		
		assertNull(task.getDeadlines());

		long taskId = taskService.addTask(task, new HashMap<String, Object>());

		taskService.executeReminderForTask(taskId, "Luke Cage");
		Thread.sleep(1000);
		assertEquals(1, wiser.getMessages().size());

		String receiver = wiser.getMessages().get(0).getEnvelopeReceiver();
		assertEquals("tony@domain.com", receiver);
		MimeMessage msg = ((WiserMessage) wiser.getMessages().get(0)) .getMimeMessage();
		assertEquals("You have a task ( Simple Test Task ) of process ( taskReminder )",
				msg.getSubject());

	}
	 
	 @Test
	 public void testTaskReminderWithNotificationByTaskNostarted() throws Exception {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("now", new Date());

		Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.ReminderWithNotificationReserved));
		InternalTask task = (InternalTask) TaskFactory.evalTask(reader, vars);
		System.out.println("testTaskReminderWithNotificationByTaskNostarted " + task.getTaskData().getStatus());

		assertEquals(1, task.getDeadlines().getEndDeadlines().size());
		assertEquals(1, task.getDeadlines().getStartDeadlines().size());
		
		long taskId = taskService.addTask(task, new HashMap<String, Object>());
		taskService.executeReminderForTask(taskId, "Luke Cage");

		long time = 0;
        while (wiser.getMessages().size() < 2 && time < 5000) {
            Thread.sleep(50);
            time += 50;
        }
		assertEquals(2, wiser.getMessages().size());

		String receiver = wiser.getMessages().get(0).getEnvelopeReceiver();
		assertEquals("tony@domain.com", receiver);
		receiver = wiser.getMessages().get(1).getEnvelopeReceiver();
		assertEquals("darth@domain.com", receiver);

		MimeMessage msg = ((WiserMessage) wiser.getMessages().get(0)).getMimeMessage();
		assertEquals("ReminderWithNotificationReserved:you have new task to be started", msg.getSubject());
		assertEquals("task is not started", msg.getContent());
		
		msg = ((WiserMessage) wiser.getMessages().get(1)).getMimeMessage();
		assertEquals("ReminderWithNotificationReserved:you have new task to be started", msg.getSubject());
		assertEquals("task is not started", msg.getContent());
	 }
	 
	 @Test
	 public void testTaskReminderWithNotificationByTaskNoCompleted() throws Exception {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("now", new Date());

		Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.ReminderWithNotificationInProgress));
		InternalTask task = (InternalTask) TaskFactory.evalTask(reader, vars);
		System.out.println("testTaskReminderWithNotificationByTaskNoCompleted " + task.getTaskData().getStatus());
		
		assertEquals(1, task.getDeadlines().getEndDeadlines().size());
		assertEquals(1, task.getDeadlines().getStartDeadlines().size());
		long taskId = taskService.addTask(task, new HashMap<String, Object>());
		taskService.executeReminderForTask(taskId, "Luke Cage");

		long time = 0;
		while (wiser.getMessages().size() < 2 && time < 5000) {
			Thread.sleep(50);
			time += 50;
		}
		assertEquals(2, wiser.getMessages().size());

		String receiver = wiser.getMessages().get(0).getEnvelopeReceiver();
		assertEquals("tony@domain.com", receiver);
		receiver = wiser.getMessages().get(1).getEnvelopeReceiver();
		assertEquals("darth@domain.com", receiver);

		MimeMessage msg = ((WiserMessage) wiser.getMessages().get(0)) .getMimeMessage();
		assertEquals("ReminderWithNotificationInProgress:you have new task to be completed",
				msg.getSubject());
		assertEquals("task is not completed", msg.getContent());

		msg = ((WiserMessage) wiser.getMessages().get(1)).getMimeMessage();
		assertEquals("ReminderWithNotificationInProgress:you have new task to be completed",
				msg.getSubject());
		assertEquals("task is not completed", msg.getContent());
	 }
}

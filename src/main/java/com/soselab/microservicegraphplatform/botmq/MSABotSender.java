package com.soselab.microservicegraphplatform.botmq;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

/*#####################################################
###	2019/12/09										###
###	created by jimting								###
###	for MSABot project, and my graduate paper QAQ	###
#######################################################*/


public class MSABotSender {

	private static final String EXCHANGE_NAME = "vmamv";


	private static final String mqip = "140.121.197.130";
	private static final int mqport = 5502;
	private static final String roomID = "C9PF9PKTL";
    
   	public boolean send(String content, String status)
	{

   		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(mqip);
		factory.setPort(mqport);
		
		JSONObject obj = new JSONObject();
        obj.put("roomID", roomID);
        obj.put("content", content);
		obj.put("status", status);
		
		String result = obj.toString();
		
		try (Connection connection = factory.newConnection();
	             	Channel channel = connection.createChannel()) {
	           	channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
			channel.basicPublish(EXCHANGE_NAME, "", null, result.getBytes("UTF-8"));
	           	System.out.println(" [x] Sent '" + result + "'");
			return true;
	        } catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return false;
	}

}



package uk.co.closemf.eclick.idcheck.route;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

public class FromMqRouteTest extends AbstractRouteTest{

	@Produce(uri = "activemq:testQueue")
	private ProducerTemplate template;
	
	@EndpointInject(uri = "mock:catchMessages")
	private MockEndpoint resultEndpoint;
		
	@DirtiesContext
    @Test
    public void fromMqRouteTest() throws Exception {

    	String body = "<CustomerDetails/>";
    	String expectedBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + System.lineSeparator() + "<CustomerDetails/>" + System.lineSeparator();
    	
        //resultEndpoint.expectedBodiesReceived(expectedBody);
        resultEndpoint.expectedMessageCount(1);
 
        template.sendBody("activemq:testQueue", body);
 
        resultEndpoint.assertIsSatisfied();
    }
	
}
package uk.co.closemf.eclick.idcheck.route;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

public class ErrorRouteTest extends AbstractRouteTest{

	@Produce(uri = "activemq:testQueue")
	private ProducerTemplate template;
	
	@EndpointInject(uri = "mock:catchErrors")
    private MockEndpoint errorEndpoint;
	
	@DirtiesContext
    @Test
    public void errorRouteTest() throws Exception {

		context.start();
        String body = "<WillFail/>";
        
        errorEndpoint.expectedBodiesReceived(body);
        errorEndpoint.expectedMessageCount(1);
 
        template.sendBody("activemq:testQueue", body);
 
        errorEndpoint.assertIsSatisfied();
        context.stop();
    }
	
}
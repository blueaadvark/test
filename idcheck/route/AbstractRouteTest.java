package uk.co.closemf.eclick.idcheck.route;

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import uk.co.closemf.eclick.idcheck.EclickIdcheckServiceApplicationTests;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = EclickIdcheckServiceApplicationTests.class)
@ActiveProfiles("test")
@UseAdviceWith(true)
@ComponentScan
public abstract class AbstractRouteTest extends CamelTestSupport{
	
	@Before
	public void mockEndpoints() throws Exception {
	   
	    XMLUnit.setIgnoreComments(true);

	    AdviceWithRouteBuilder mockDestination = new AdviceWithRouteBuilder() {

	        @Override
	        public void configure() throws Exception {
	            interceptSendToEndpoint("http://localhost:8080/notSure")
	                .skipSendToOriginalEndpoint()
	                .to("mock:catchMessages");
	        }
	    };
	    context.getRouteDefinitions().get(0)
	        .adviceWith(context, mockDestination);
	    
	    AdviceWithRouteBuilder mockMqResponse = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("activemq:response")
                    .skipSendToOriginalEndpoint()
                    .to("mock:mqResponse");
            }
        };
        context.getRouteDefinitions().get(0)
            .adviceWith(context, mockMqResponse);
	    
	    AdviceWithRouteBuilder mockErrorDestination = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("direct:DLC")
                    .skipSendToOriginalEndpoint()
                    .to("mock:catchErrors");
            }
        };
        context.getRouteDefinitions().get(0)
            .adviceWith(context, mockErrorDestination);
        
        
	}

	protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        // START SNIPPET: example
        camelContext.addComponent("activemq", activeMQComponent("vm://localhost?broker.persistent=false"));
        // END SNIPPET: example

        return camelContext;
	}
	
	
	@Override
	protected RouteBuilder createRouteBuilder() {
	    return new IdCheckRoute();
	}

	 @After
	 public void tearDown() throws Exception {
	    context.stop();
	    context = (ModelCamelContext) createCamelContext();
	 }
}
package uk.co.closemf.eclick.idcheck.route;

import static org.apache.camel.component.jms.JmsComponent.jmsComponentAutoAcknowledge;

import javax.jms.ConnectionFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import uk.co.closemf.eclick.dto.internal.CustomerDetails;
import uk.co.closemf.eclick.dto.internal.IDCheckResponse;
import uk.co.closemf.eclick.idcheck.test.util.CamelJmsTestHelper;
import uk.co.closemf.eclick.idcheck.test.util.TestUtil;

@RunWith(CamelSpringBootRunner.class)
//@SpringBootTest(classes = EclickIdcheckServiceApplicationTests.class)
//@SpringBootTest(classes = IdCheckRoute.class)
//@SpringBootTest(classes = IdCheckDLCRoute.class)
@SpringBootTest(classes = { IdCheckDLCRoute.class, IdCheckRoute.class})
@ActiveProfiles("test")
@EnableAutoConfiguration
@UseAdviceWith(true)
@ComponentScan
public class IdCheckRoutingIntTest extends CamelTestSupport{
	
	//private RouteBuilder route;
	@Autowired
	IdCheckRoute idCheckRoute;

    @Produce(uri = "activemq:testQueue")
    private ProducerTemplate template;
    
    @EndpointInject(uri = "mock:catchMessages")
    private MockEndpoint resultEndpoint;
    
    @EndpointInject(uri = "mock:mqResponse")
    private MockEndpoint mqResponseEndpoint;
    
    @EndpointInject(uri = "mock:catchErrors")
    private MockEndpoint errorEndpoint;
    
    @Value("classpath:idCheckRequest.xml")
    private Resource idCheckRequestXml;
    
    @Value("classpath:idCheckResponse.xml")
    private Resource idCheckResponseXml;
    
    @Value("${activemq.queue.input:wibble}")
    private String mqInput;
    
    @Autowired
    protected CamelContext camelContext;

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

        ConnectionFactory connectionFactory = CamelJmsTestHelper.createConnectionFactory();
        camelContext.addComponent("activemq", jmsComponentAutoAcknowledge(connectionFactory));

        return camelContext;
    }
  
    @DirtiesContext
    @Test
    public void fromMqRouteTest() throws Exception {

        String body = "<CustomerDetails/>";
        String expectedBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + System.lineSeparator() + "<CustomerDetails/>" + System.lineSeparator();

        resultEndpoint.expectedMessageCount(1);
 
        template.sendBody("activemq:testQueue", body);
        resultEndpoint.assertIsSatisfied();
        
        String receivedDetails = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        Diff diff = new Diff(expectedBody, receivedDetails);
        assertTrue(diff.similar());

    }
    
    @DirtiesContext
    @Test
    @Ignore
    public void errorRouteTest() throws Exception {

    	context.start();
        String body = "<WillFail/>";
        
        errorEndpoint.expectedBodiesReceived(body);
        errorEndpoint.expectedMessageCount(1);
 
        template.sendBody("activemq:testQueue", body);
 
        errorEndpoint.assertIsSatisfied();
        context.stop();
    }
    
    @DirtiesContext
    @Test
    public void fromMqRouteWithCustomerDetailsTest() throws Exception {

        CustomerDetails expectedDetails = TestUtil.getTestDtoFromXML(CustomerDetails.class, idCheckRequestXml);

        resultEndpoint.expectedMessageCount(1);
        
        String requestXmlString = TestUtil.getStringFromFile(idCheckRequestXml);
 
        template.sendBody("activemq:testQueue", requestXmlString);
        
        resultEndpoint.assertIsSatisfied();
        
        CustomerDetails receivedDetails = resultEndpoint.getExchanges().get(0).getIn().getBody(CustomerDetails.class);

        assertEquals(expectedDetails, receivedDetails);
       
    }
    
    @DirtiesContext
    @Test
    public void mqResponseHasIDCheckResponseTest() throws Exception {
        
        mqResponseEndpoint.expectedMessageCount(1);
        
        String requestXmlString = TestUtil.getStringFromFile(idCheckResponseXml);
 
        template.sendBody("activemq:testQueue", requestXmlString);
        
        mqResponseEndpoint.assertIsSatisfied();
        
        String receivedDetails = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        Diff diff = new Diff(requestXmlString, receivedDetails);
        assertTrue(diff.similar());

    }
    
    @DirtiesContext
    @Test
    public void endpointHasIDCheckResponseTest() throws Exception {
        
        IDCheckResponse expectedDetails = TestUtil.getTestDtoFromXML(IDCheckResponse.class, idCheckResponseXml);

        resultEndpoint.expectedMessageCount(1);
        
        String requestXmlString = TestUtil.getStringFromFile(idCheckResponseXml);
 
        template.sendBody("activemq:testQueue", requestXmlString);
        
        resultEndpoint.assertIsSatisfied();
        
        IDCheckResponse receivedDetails = resultEndpoint.getExchanges().get(0).getIn().getBody(IDCheckResponse.class);

        assertEquals(expectedDetails, receivedDetails);

    }

    @Override
    protected RouteBuilder createRouteBuilder() {
    	HttpComponent httpComponent = new HttpComponent();
        context().addComponent("http", httpComponent);
    	return idCheckRoute;
    }

}
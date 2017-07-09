package uk.co.closemf.eclick.idcheck.route;

import javax.xml.bind.JAXBContext;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spi.DataFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import uk.co.closemf.eclick.dto.internal.CustomerDetails;
import uk.co.closemf.eclick.dto.internal.IDCheckResponse;


@Component
public class IdCheckRoute extends RouteBuilder {
    
    public static final String ROUTE_ID = "IdCheckRoute";
    
    @Value("${activemq.queue.input:activemq:testQueue}")
    private String mqInput;
    
    @Value("${rest.idcheck.endpoint:http://localhost:8080/notSure}")
    private String restEndpoint;

      
	@Override
	public void configure() throws Exception {
	    
	    JAXBContext jaxbContext = JAXBContext.newInstance(CustomerDetails.class, IDCheckResponse.class);
	    DataFormat jaxb = new JaxbDataFormat(jaxbContext);
	       
	    HttpComponent httpComponent = new HttpComponent();
	    getContext().addComponent("http", httpComponent);
    
	    errorHandler(deadLetterChannel("direct:DLC").useOriginalMessage());
	    
		//from("{{activemq.queue.input}}") //TODO: pick up from properties
	    //from("activemq:testQueue")
		from(mqInput)
		    .log("${body}")
		    .unmarshal(jaxb)
		    .setHeader(Exchange.HTTP_METHOD, constant("POST"))
		    .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
		    .to(restEndpoint) //TODO: pick up from properties
		    //.to("{{rest.idcheck.endpoint}}") //TODO: pick up from properties
		    .marshal(jaxb)
		    .to("activemq:response");
		
	}	

}

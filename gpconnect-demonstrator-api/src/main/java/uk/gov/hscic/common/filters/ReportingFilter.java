package uk.gov.hscic.common.filters;

import com.google.common.base.Charsets;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;
import uk.gov.hscic.common.filters.helper.RequestWrapper;

@Component
@WebFilter(urlPatterns = "/fhir/*")
public class ReportingFilter implements Filter {

    @Value("${reportDir:C:/Temp/}")
    private String reportDir;

    private static final String SSP_FROM = "Ssp-From";
    private static final String SSP_INTERACTIONID = "Ssp-InteractionID";
    private static final String SSP_TO = "Ssp-To";
    private static final String SSP_TRACEID = "Ssp-TraceId";

    @Override
    public void init(FilterConfig fConfig) throws ServletException {
        int i = 0;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;

        // Create report
        String reportString = "GP Connect request Report";

        if(null == httpReq.getQueryString()){
            reportString = reportString + "\n\nQuery: " + httpReq.getRequestURI();
        } else {
            reportString = reportString + "\n\nQuery: " + httpReq.getRequestURI() + "?" + httpReq.getQueryString();
        }
        
        // Print request headers
        reportString = reportString + "\n\nRequest Headers:";
        Enumeration<String> headerNames = httpReq.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            reportString = reportString + "\n" + headerName + " : " + httpReq.getHeader(headerName);
        }

        // Store payload
        
        reportString = reportString + "\n\nRequest Payload:";
        
        HttpServletRequest properRequest = ((HttpServletRequest) request);
        RequestWrapper wrappedRequest = new RequestWrapper(properRequest);
        request = wrappedRequest;

        BufferedReader br = request.getReader();
        String line = br.readLine();
        while (line != null) {
            reportString = reportString + "\n" + line;
            line = br.readLine();
        }

        httpReq.setAttribute("Report", reportString);
                
        
        HttpServletResponse res = (HttpServletResponse) response;
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(res);
        
        // Pass on request
        chain.doFilter(request, wrapper);
        
        reportString = reportString + "\n\n\nResponse Code: " + res.getStatus();
        
        reportString = reportString + "\n\nResponse Headers:";
        for (String headName : res.getHeaderNames()) {
            reportString = reportString + "\n" + headName + " : " + res.getHeaders(headName);
        }

        // Print response payload
        reportString = reportString + "\n\nResponse Body:\n" + new String(wrapper.getContentAsByteArray(),Charsets.UTF_8);
        wrapper.copyBodyToResponse();
        
        httpReq.setAttribute("Report", reportString);
        
        // Save report        
        storeReport(httpReq);
    }

    @Override
    public void destroy() {
        int i = 0;
    }

    public void storeReport(HttpServletRequest theServletRequest) {

        String report = (String) theServletRequest.getAttribute("Report");
        try {
            FileWriter fileWriter = new FileWriter(reportDir + theServletRequest.getHeader(SSP_TRACEID) + ".txt");
            fileWriter.write(report);
            fileWriter.close();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FhirRequestGenericIntercepter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

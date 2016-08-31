package com.capitalone.dashboard.collector;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.BuildStatus;
import com.capitalone.dashboard.util.Supplier;

@RunWith(MockitoJUnitRunner.class)
public class DefaultTravisCiClientTests {
    @Mock private Supplier<RestOperations> restOperationsSupplier;
    @Mock private RestOperations rest;
    private DefaultTravisCiClient client; 
    
    @Before
    public void init() {
        when(restOperationsSupplier.get()).thenReturn(rest);
    	client = new DefaultTravisCiClient(restOperationsSupplier);
    }

    @Test
    public void getBuildInformation() throws Exception {
        when(rest.exchange(Matchers.any(URI.class), eq(HttpMethod.GET), Matchers.any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(getJson("buildDetails_full.json"), HttpStatus.OK));
        List<Build> buildDetails = client.getBuildDetails("https://gitlab.pramati.com/Imaginea/KodeBeagle");
        assertThat(buildDetails.size(), is(25));
        Build firstBuild = buildDetails.get(0);
        assertThat(firstBuild.getBuildStatus(), is(BuildStatus.Success));
    }

    @Test
    public void getTime() throws Exception {
    	String field = "finished_at";
    	long time = client.getTime(createJSON(field, "2016-08-29T07:42:31Z"), field);
    	assert(time > 1000);
    }

    @Test
    public void buildRestURL() {
    	String restUrl = client.buildRestURL("https://github.com/Imaginea/KodeBeagle");
    	assertEquals("https://api.travis-ci.org/repo/Imaginea%2FKodeBeagle/builds", restUrl);
    }
    
	@SuppressWarnings("unchecked")
	private JSONObject createJSON(String field, String value) {
		JSONObject object = new JSONObject();
		object.put(field, value);
		return object;
	}
	
   private String getJson(String fileName) throws IOException {
        InputStream inputStream = DefaultTravisCiClientTests.class.getResourceAsStream(fileName);
        return IOUtils.toString(inputStream);
    }

}
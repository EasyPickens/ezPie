package com.rest.wrapper.ezpie.warmpie.controllers;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-09-27
 * 
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

//import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.fanniemae.ezpie.JobManager;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@Api(value = "PieControllerAPI", produces = MediaType.APPLICATION_JSON_VALUE)
public class PieController {

	@Value("${ezpie.settings}")
	private String _pathToSettings;

	
	@GetMapping("/")
	public RedirectView redirect() {
		return new RedirectView("/swagger-ui.html");
	}
	
	@GetMapping("/api/v1/status")
	@ApiOperation("Returns the status of the API service.")
	@ApiResponses(value = {@ApiResponse(code = 200, message="OK")})
	public String getStatus() {
		return "WarmPie REST API is up and running.";
	}

	@GetMapping("/api/v1/runjob/{jobname}")
	@ApiOperation("Run an existing job definition by providing the job name.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message="OK"),
			@ApiResponse(code = 400, message="Bad Request")
			})
	public ResponseEntity<Object> runJob(@PathVariable String jobname) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		System.out.println(String.format("%s: INFO Path to settings file: %s", sdf.format(Calendar.getInstance().getTime()), _pathToSettings));
		System.out.println(String.format("%s: INFO GET request to run %s job definition.", sdf.format(Calendar.getInstance().getTime()), jobname));

		JobManager jm = new JobManager(_pathToSettings, jobname, null);
		String data = jm.getDataJson();
		System.out.println(String.format("%s: INFO job %s processing completed.", sdf.format(Calendar.getInstance().getTime()), jobname));
		return new ResponseEntity<Object>(data, HttpStatus.OK);
	}

	@PostMapping("/api/v1/runjob/{jobname}")
	@ApiOperation("POST version that provides the ability to pass addition parameters into the job defintion.  Job definition must already exist.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message="OK"),
			@ApiResponse(code = 400, message="Bad Request")
			})
	public ResponseEntity<Object> postRunJob(@PathVariable String jobname, @RequestBody(required = false) String jsonParameters) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		System.out.println(String.format("%s: INFO Path to settings file: %s", sdf.format(Calendar.getInstance().getTime()), _pathToSettings));
		System.out.println(String.format("%s: INFO POST request to run %s job definition.", sdf.format(Calendar.getInstance().getTime()), jobname));

		JobManager jm = new JobManager(_pathToSettings, jobname, null);

		if ((jsonParameters != null) && (!"".equals(jsonParameters))) {
			// Convert the key value pairs into Tokens.
			System.out.println(jsonParameters.getClass().getName());
			System.out.println(String.format("%s: INFO POST request body: %s", sdf.format(Calendar.getInstance().getTime()), jsonParameters));
			try {
				JSONObject json = new JSONObject(jsonParameters);
				
				Map<String, String> tokens = new HashMap<String, String>();
				Iterator<String> keys = json.keys();
				while (keys.hasNext()) {
					String key = (String) keys.next();
					tokens.put(key, json.optString(key));
				}
				jm.addTokens(tokens);
			} catch (JSONException ex) {
				throw new RuntimeException("Message body could not be converted into JSON. " + ex.getMessage());
			}
		}

		String data = jm.getDataJson();
		System.out.println(String.format("%s: INFO job %s processing completed.", sdf.format(Calendar.getInstance().getTime()), jobname));
		return new ResponseEntity<Object>(data, HttpStatus.OK);
	}

}

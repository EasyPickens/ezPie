package com.rest.wrapper.ezpie.warmpie.controllers;

import java.text.SimpleDateFormat;
import java.util.Calendar;

//import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fanniemae.ezpie.JobManager;

@RestController
public class PieController {

	@Value("${ezpie.settings}")
	private String _pathToSettings;

	@GetMapping("/api/v1/status")
	public String getStatus() {
		return "WarmPie REST API is up and running.";
	}

	@GetMapping("/api/v1/runjob/{jobname}")
	public ResponseEntity<Object> runJob(@PathVariable String jobname) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		System.out.println(String.format("%s: INFO GET request to run %s job definition.", sdf.format(Calendar.getInstance().getTime()), jobname));
		System.out.println(String.format("%s: INFO Path to settings file: %s", sdf.format(Calendar.getInstance().getTime()), _pathToSettings));

		JobManager jm = new JobManager(_pathToSettings, jobname, null);
		String data = jm.getDataJson();
		System.out.println(String.format("%s: INFO job %s processing completed.", sdf.format(Calendar.getInstance().getTime()), jobname));
		return new ResponseEntity<Object>(data,HttpStatus.OK);
	}

}

package com.dji.sample.flightauthorization.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dji.sample.flightauthorization.api.view.FlightAuthorizationListView;
import com.dji.sample.flightauthorization.applicationservice.FlightAuthorizationApplicationService;
import com.dji.sample.flightauthorization.ussp.view.FlightAuthorizationRequestView;

import lombok.NonNull;

@RestController
@RequestMapping("${url.flight-authorization-request.version}${url.flight-authorization-request.prefix}")
public class FlightAuthorizationRequestController {

	private final FlightAuthorizationRequestGuard guard;
	private final FlightAuthorizationApplicationService applicationService;

	public FlightAuthorizationRequestController(
		@NonNull FlightAuthorizationRequestGuard flightAuthorizationRequestGuard,
		@NonNull FlightAuthorizationApplicationService flightAuthorizationApplicationService) {
		this.guard = flightAuthorizationRequestGuard;
		this.applicationService = flightAuthorizationApplicationService;
	}

	@GetMapping("{workspace_id}")
	public List<FlightAuthorizationListView> getAllRequests(
		@PathVariable("workspace_id") String workspaceId,
		HttpServletRequest request) {
		guard.getAllRequests(workspaceId, request);
		return applicationService.getAllRequests();
	}

	@GetMapping("{workspace_id}/{id}")
	public ResponseEntity<FlightAuthorizationRequestView> getRequest(
		@PathVariable("workspace_id") String workspaceId,
		@PathVariable("id") Long id,
		HttpServletRequest request) {
		guard.getRequest(workspaceId, id, request);
		return applicationService.getRequest(id);
	}

	@PostMapping("{workspace_id}/create")
	public void createRequest(
		@PathVariable("workspace_id") String workspaceId,
		HttpServletRequest request) {
	}

	@PutMapping("{workspace_id}/{id}/cancel")
	public void cancelRequest(
		@PathVariable("workspace_id") String workspaceId,
		@PathVariable("id") Long id) {
	}
}

package com.chatgpt.integrationCodegpt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WorkOrderController {

	@Autowired
    private WorkOrderAdapter workOrderAdapter;

    public WorkOrderController(WorkOrderAdapter workOrderAdapter) {
        this.workOrderAdapter = workOrderAdapter;
    }

    @GetMapping("/workorder/{id}")
    public ResponseEntity<WorkOrder> getWorkOrderById(@PathVariable String id) {
        try {
            WorkOrder workOrder = workOrderAdapter.fetchWorkOrderById(id);
            return ResponseEntity.ok(workOrder);
        } catch (Exception e) {
            // handle exceptions and return appropriate error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

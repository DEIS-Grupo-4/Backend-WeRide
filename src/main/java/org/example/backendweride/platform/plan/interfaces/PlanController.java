package org.example.backendweride.platform.plan.interfaces;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.backendweride.platform.plan.domain.queries.GetPlanById;
import org.example.backendweride.platform.plan.domain.services.commands.PlanCommandService;
import org.example.backendweride.platform.plan.domain.services.queries.PlanQueryService;
import org.example.backendweride.platform.plan.interfaces.resources.CreatePlanResource;
import org.example.backendweride.platform.plan.interfaces.resources.PlanResource;
import org.example.backendweride.platform.plan.interfaces.transform.CreatePlanCommandFronResourceAssembler;
import org.example.backendweride.platform.plan.interfaces.transform.PlanResourceFromEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/plans", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Plans", description = "Manage subscription plans")
public class PlanController {

    private final PlanCommandService planCommandService;
    private final PlanQueryService planQueryService;

    /**
     * Creates the plan controller with its application services.
     *
     * @param planCommandService service for plan commands
     * @param planQueryService service for plan queries
     */
    public PlanController(PlanCommandService planCommandService, PlanQueryService planQueryService) {
        this.planCommandService = planCommandService;
        this.planQueryService = planQueryService;
    }

    /**
     * Creates a new subscription plan.
     *
     * @param planResource information required to create the plan
     * @return response containing the created plan
     */
    @PostMapping
    public ResponseEntity<PlanResource> createPlan(@RequestBody CreatePlanResource planResource) {
        var result = this.planCommandService.handle(
                CreatePlanCommandFronResourceAssembler.toCommandFromResource(planResource)
        );
        return result.map(response -> new ResponseEntity<>(
                PlanResourceFromEntity.toPlanResource(response), HttpStatus.CREATED
        )).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());

    }

    /**
     * Retrieves a subscription plan by its identifier.
     *
     * @param id plan identifier
     * @return response containing the requested plan
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanResource> findPlanById(@PathVariable Long id) {
        var result = this.planQueryService.handle(new GetPlanById(id));
        return result.map(response -> new ResponseEntity<>(
                PlanResourceFromEntity.toPlanResource(response), HttpStatus.OK
        )).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Retrieves all available subscription plans.
     *
     * @return response containing the available plans
     */
    @GetMapping
    public ResponseEntity<List<PlanResource>> findAllPlans() {
        var result = this.planQueryService.handle();
        return result.map(response -> new ResponseEntity<>(
                response, HttpStatus.OK
                )).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Updates an existing subscription plan.
     *
     * @param id plan identifier
     * @param resource updated plan information
     * @return response containing the updated plan
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlanResource> updatePlan(@PathVariable Long id, @RequestBody CreatePlanResource resource) {
        return planCommandService.handle(id, CreatePlanCommandFronResourceAssembler.toCommandFromResource(resource))
                .map(PlanResourceFromEntity::toPlanResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a subscription plan by its identifier.
     *
     * @param id plan identifier
     * @return response indicating the deletion result
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlanById(@PathVariable Long id) {
        boolean deleted = this.planCommandService.handle(id);
        return deleted
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

}

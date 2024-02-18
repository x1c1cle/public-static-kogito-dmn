
package org.rhett;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ObjectBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.dmg.pmml.Decision;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.kogito.Application;
import org.kie.kogito.dmn.rest.DMNJSONUtils;
import org.kie.kogito.dmn.rest.DecisionDMNResult;
import org.kie.kogito.dmn.rest.KogitoDMNResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.kie.kogito.dmn.rest.KogitoDMNMessage;

@RestController
@RequestMapping("/decisions")
public class CustomController {

    @org.springframework.beans.factory.annotation.Autowired()
    Application application;

    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()).registerModule(new com.fasterxml.jackson.databind.module.SimpleModule().addSerializer(org.kie.dmn.feel.lang.types.impl.ComparablePeriod.class, new org.kie.kogito.dmn.rest.DMNFEELComparablePeriodSerializer())).disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);

    @PostMapping(value = "/execute", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> executeRules(@RequestBody(required = false) java.util.Map<String, Object> variables) {
        String dmnName = (String)variables.get("DmnName");
        String dmnNamespace = (String)variables.get("DmnNamespace");
        org.kie.kogito.decision.DecisionModel decision = application.get(org.kie.kogito.decision.DecisionModels.class).getDecisionModel(dmnNamespace, dmnName);
        org.kie.dmn.api.core.DMNResult decisionResult = decision.evaluateAll(DMNJSONUtils.ctx(decision, variables));
        DecisionDMNResult result = new DecisionDMNResult(dmnNamespace, dmnName, decisionResult);
        return enrichResponseHeaders(decisionResult, extractContextIfSucceded(result));
    }

    private Entry<HttpStatus, ?> extractContextIfSucceded(DecisionDMNResult result) {
        if (!result.hasErrors()) {
            return new SimpleEntry(HttpStatus.OK, buildResponse(result.getDmnContext()));
        } else {
            return buildFailedEvaluationResponse(result);
        }
    }

    private String buildResponse(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private ResponseEntity enrichResponseHeaders(org.kie.dmn.api.core.DMNResult result, Entry<HttpStatus, ?> response) {
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.status(response.getKey());
        if (!result.getMessages().isEmpty()) {
            String infoWarns = result.getMessages().stream().map(m -> m.getLevel() + " " + m.getMessage()).collect(Collectors.joining(", "));
            bodyBuilder.header("X-Kogito-decision-messages", infoWarns);
        }
        org.kie.kogito.decision.DecisionExecutionIdUtils.getOptional(result.getContext()).ifPresent(executionId -> bodyBuilder.header("KOGITO_EXECUTION_ID_HEADER", executionId));
        return bodyBuilder.body(response.getValue());
    }

    private Entry<HttpStatus, ?> buildFailedEvaluationResponse(DecisionDMNResult result) {
        
        List<org.kie.dmn.api.core.DMNMessage> msgs = result.getMessages();
        for(int i=0;i< msgs.size();i++) {
            org.kie.dmn.api.core.DMNMessage dmnMessage = msgs.get(i);

            if (dmnMessage.getMessage().contains("date()")) {
                result.customMessages.add(new CustomMessage(dmnMessage.getSeverity().toString(),"Error reading in variable of type date.",
                    "Custom Message Type","Custom Source Id"));
            } else {
                result.customMessages.add(new CustomMessage(dmnMessage.getSeverity().toString(),dmnMessage.getMessage().toString(),
                        dmnMessage.getMessageType().toString(),dmnMessage.getSourceId().toString()));
                }

        }

        return new SimpleEntry(HttpStatus.INTERNAL_SERVER_ERROR, result);
    }


    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("healthy");
    }
    
}

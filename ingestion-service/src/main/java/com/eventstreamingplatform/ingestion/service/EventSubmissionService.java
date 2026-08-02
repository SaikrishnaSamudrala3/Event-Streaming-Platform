package com.eventstreamingplatform.ingestion.service;

import com.eventstreamingplatform.ingestion.api.CreateOrderEventRequest;
import com.eventstreamingplatform.ingestion.api.EventAcceptedResponse;

public interface EventSubmissionService {

    EventAcceptedResponse submit(
            CreateOrderEventRequest request,
            String correlationId);
}

package eu.bbmri.eric.csit.service.model.request.form;

import eu.bbmri.eric.csit.service.model.request.Request;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

public class FormTemplate {
    @ManyToOne
    @JoinColumn(name = "request_id")
    private Request request;
}

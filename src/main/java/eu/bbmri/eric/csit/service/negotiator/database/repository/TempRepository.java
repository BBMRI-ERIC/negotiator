package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Temp;

import java.util.ArrayList;

public interface TempRepository {
    Temp findById(int id);
    Temp save(Temp temp);

    ArrayList<Temp> findAll();
}

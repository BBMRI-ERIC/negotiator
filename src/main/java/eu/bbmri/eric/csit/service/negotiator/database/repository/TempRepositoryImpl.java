package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Temp;

import java.util.ArrayList;
import java.util.HashMap;

public class TempRepositoryImpl implements TempRepository {

    private final HashMap<Integer, Temp> negotiations = new HashMap<>();
    @Override
    public Temp findById(int id) {
        return negotiations.get(id);
    }

    @Override
    public Temp save(Temp temp) {
        negotiations.put(temp.getId(), temp);
        return temp;
    }

    @Override
    public ArrayList<Temp> findAll() {
        return new ArrayList<>(negotiations.values());
    }
}

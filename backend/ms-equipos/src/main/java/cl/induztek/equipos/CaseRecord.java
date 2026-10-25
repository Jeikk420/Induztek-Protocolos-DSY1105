package cl.induztek.equipos;
import com.fasterxml.jackson.databind.JsonNode;
public record CaseRecord(String kind,String id,int revision,JsonNode payload) {}

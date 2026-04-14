package com.o360.sastplugin.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScanResponse {

    @JsonProperty("projectId")
    private String projectId;

    @JsonProperty("status")
    private int status;

    @JsonProperty("vulnerabilities")
    private List<Vulnerability> vulnerabilities;

    @JsonProperty("totalVulnerabilities")
    private int totalVulnerabilities;

    public ScanResponse() {
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public List<Vulnerability> getVulnerabilities() { return vulnerabilities; }
    public void setVulnerabilities(List<Vulnerability> vulnerabilities) { this.vulnerabilities = vulnerabilities; }

    public int getTotalVulnerabilities() { return totalVulnerabilities; }
    public void setTotalVulnerabilities(int totalVulnerabilities) { this.totalVulnerabilities = totalVulnerabilities; }
}

package gov.va.mass.monitoring;

/**
 * Created by n_nac on 10/26/2017.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MicroService {

    private String name;
    private String url;
    private String pulse;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPulse() {
        return pulse;
    }

    public void setPulse(String blog) {
        this.pulse = pulse;
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Service [ name=" + name + ", url = "+ url +", pulse=" + pulse + "]";
    }

}
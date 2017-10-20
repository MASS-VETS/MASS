package gov.va.mass.adapter.microcontroller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class serviceEndPoint {
        private String type;

        public serviceEndPoint() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "Quote{" +
                    "type='" + type + '\'' +
                    '}';
        }
    }



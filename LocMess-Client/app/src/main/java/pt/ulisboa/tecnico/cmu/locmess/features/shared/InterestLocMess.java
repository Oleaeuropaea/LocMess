package pt.ulisboa.tecnico.cmu.locmess.features.shared;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class InterestLocMess implements Serializable {
    private String url;
    private String name;
    private String value;

    public InterestLocMess(String url, String name, String value) {
        this.url = url;
        this.name = name;
        this.value = value;
    }

    public InterestLocMess(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public InterestLocMess(String name) {
        this.name = name;
    }

    public String getUrl() { return url; }

    public String getName() { return name; }

    public String getValue() { return value; }

    public void setName(String name) { this.name = name; }

    public void setUrl(String url) { this.url = url; }

    public void setValue(String value) { this.value = value; }

    @Override
    public String toString() {
        if (value == null) { return name; }
        return getName() + ":" + value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }

        InterestLocMess interest = (InterestLocMess) obj;

        return new EqualsBuilder()
                .append(name, interest.name)
                .append(value, interest.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(name)
                .append(value)
                .toHashCode();
    }
}

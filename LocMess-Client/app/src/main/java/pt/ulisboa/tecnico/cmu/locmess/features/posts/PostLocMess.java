package pt.ulisboa.tecnico.cmu.locmess.features.posts;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.InterestLocMess;

public class PostLocMess implements Serializable {
    public final static String WHITE_LIST = "WL";
    public final static String BLACK_LIST = "BL";

    private String url;
    private String subject;
    private DateTime creation_date;
    private String sender;
    private String sender_email;
    private String sender_device_name;
    private LocationLocMess location;
    private String policy;
    private DateTime start_date;
    private DateTime end_date;
    private List<InterestLocMess> interests;
    private boolean centralized_mode = true;
    private String content;

    public PostLocMess(String subject, DateTime creationDate, LocationLocMess location, String policy,
                       DateTime startDate, DateTime endDate, List<InterestLocMess> interests,
                       String content, boolean centralized_mode) {
        this.subject = subject;
        this.creation_date = creationDate;
        this.location = location;
        this.policy = policy;
        this.start_date = startDate;
        this.end_date = endDate;
        this.interests = interests;
        this.content = content;
        this.centralized_mode = centralized_mode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public DateTime getCreationDate() {
        return creation_date;
    }

    public void setCreationDate(DateTime creation_date) {
        this.creation_date = creation_date;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderEmail() {
        return sender_email;
    }

    public void setSenderEmail(String sender_email) {
        this.sender_email = sender_email;
    }

    public String getSenderDeviceName() {
        return sender_device_name;
    }

    public void setSenderDeviceName(String sender_dev_name) {
        this.sender_device_name = sender_dev_name;
    }

    public LocationLocMess getLocation() {
        return location;
    }

    public void setLocation(LocationLocMess location) {
        this.location = location;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public DateTime getStartDate() {
        return start_date;
    }

    public void setStartDate(DateTime start_date) {
        this.start_date = start_date;
    }

    public DateTime getEndDate() {
        return end_date;
    }

    public void setEndDate(DateTime end_date) {
        this.end_date = end_date;
    }

    public List<InterestLocMess> getInterests() {
        return interests;
    }

    public void setInterests(List<InterestLocMess> interests) {
        this.interests = interests;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isCentralizedMode() {
        return centralized_mode;
    }

    public String getDeliveryMode() {
        return isCentralizedMode() ? "Centralized" : "Decentralized";
    }

    @Override
    public String toString() {
        return subject;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false;}
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }

        PostLocMess post = (PostLocMess) obj;
        return new EqualsBuilder()
                .append(url, post.url)
                .append(subject, post.subject)
                .append(sender, post.sender)
                .append(policy, post.policy)
                .append(start_date, post.start_date)
                .append(end_date, post.end_date)
                .append(interests, post.interests)
                .append(content, post.content)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(url)
                .append(subject)
                .append(sender)
                .append(policy)
                .append(start_date)
                .append(end_date)
                .append(interests)
                .append(content)
                .toHashCode();
    }
}
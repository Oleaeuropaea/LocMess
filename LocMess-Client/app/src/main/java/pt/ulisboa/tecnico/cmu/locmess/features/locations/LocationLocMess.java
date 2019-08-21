package pt.ulisboa.tecnico.cmu.locmess.features.locations;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class LocationLocMess implements Serializable {
    private String url;
    private int id;
    private String type;
    private String name;
    private float latitude;
    private float longitude;
    private float radius;
    private String ssid;

    public enum LocationType {
        WIFI("WIFI"),
        GPS("GPS"),
        BLE("BLE");
        private String type;

        LocationType(String type){
            this.type = type;
        }

        public String getType(){
            return this.type;
        }
    }

    public LocationLocMess(String url, int id, String name, float latitude,
                           float longitude, float radius) {
        this.url = url;
        this.id = id;
        this.type = LocationType.GPS.getType();
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }


    public LocationLocMess(String url, String name, float latitude,
                           float longitude, float radius) {
        this.url = url;
        this.type = LocationType.GPS.getType();
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public LocationLocMess(String url, int id, LocationType type, String name, String ssid) {
        this.url = url;
        this.id = id;
        this.type = type.getType();
        this.name = name;
        this.ssid = ssid;
    }

    public LocationLocMess(String url, LocationType type, String name, String ssid) {
        this.url = url;
        this.type = type.getType();
        this.name = name;
        this.ssid = ssid;
    }

    private LocationLocMess(String name) {
        this.name = name;
    }

    public static LocationLocMess newDummyInstance() {
        return new LocationLocMess("---");
    }

    public boolean isDummy() {
        return name.equals("---");
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false;}
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }

        LocationLocMess location = (LocationLocMess) obj;

        return new EqualsBuilder()
                .append(id, location.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .toHashCode();
    }
}

package com.example.sossystem;

import java.util.List;

public class SosRecords {

    String RecordID;
    String FullName;
    String PhoneNumber;
    String EmailAddress;
//    List<Double> latitudeList;
//    List<Double> longitudeList;
    Double Latitude;
    Double Longitude;
    String CallFor;


    public SosRecords(){

    }

    public SosRecords(String fullName, String phoneNumber, String userEmail, Double latitude, Double longitude, String callFor) {

        FullName = fullName;
        PhoneNumber = phoneNumber;
        EmailAddress = userEmail;
        Latitude = latitude;
        Longitude = longitude;
        CallFor = callFor;
    }

    public String getRecordID() {
        return RecordID;
    }

    public void setRecordID(String recordID) {
        RecordID = recordID;
    }

    public String getFullName() {
        return FullName;
    }

    public void setFullName(String fullName) {
        FullName = fullName;
    }

    public String getPhoneNumber() {
        return PhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        PhoneNumber = phoneNumber;
    }

    public String getEmailAddress() {
        return EmailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        EmailAddress = emailAddress;
    }

    public Double getLatitude() {
        return Latitude;
    }

    public void setLatitude(Double latitude) {
        Latitude = latitude;
    }

    public Double getLongitude() {
        return Longitude;
    }

    public void setLongitude(Double longitude) {
        Longitude = longitude;
    }

    public String getCallFor() {
        return CallFor;
    }

    public boolean setCallFor(String callFor) {
        CallFor = callFor;
        return false;
    }

}

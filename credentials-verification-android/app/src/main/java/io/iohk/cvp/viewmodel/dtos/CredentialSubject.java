package io.iohk.cvp.viewmodel.dtos;

import lombok.Data;

@Data
public class CredentialSubject {

    private String id;
    private String name;
    private String dateOfBirth;
    private String identityNumber;
    private String sex;
    private String nationality;
    private String degreeAwarded;
    private String degreeResult;
    private String graduationYear;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getSex() {
        return sex;
    }

    public String getNationality() {
        return nationality;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }

    public String getDegreeAwarded() {
        return degreeAwarded;
    }

    public void setDegreeAwarded(String degreeAwarded) {
        this.degreeAwarded = degreeAwarded;
    }

    public String getDegreeResult() {
        return degreeResult;
    }

    public void setDegreeResult(String degreeResult) {
        this.degreeResult = degreeResult;
    }

    public String getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(String graduationYear) {
        this.graduationYear = graduationYear;
    }
}

package io.iohk.atala.prism.app.viewmodel.dtos;

import lombok.Data;
import lombok.Getter;

@Data
public class CredentialSubject {

    @Getter
    private String id;

    @Getter
    private String name;

    @Getter
    private String dateOfBirth;

    @Getter
    private String identityNumber;

    @Getter
    private String sex;

    @Getter
    private String nationality;

    @Getter
    private String degreeAwarded;

    @Getter
    private String degreeResult;

    @Getter
    private String graduationYear;

}

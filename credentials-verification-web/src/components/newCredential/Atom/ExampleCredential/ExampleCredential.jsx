import React from 'react';
import { Col } from 'antd';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import {
  EXAMPLE_DEGREE_NAME,
  EXAMPLE_UNIVERSITY_NANE,
  EXAMPLE_AWARD,
  EXAMPLE_FULL_NAME,
  EXAMPLE_AWARD_START_DATE,
  EXAMPLE_GRADUATION_DATE
} from '../../../../helpers/constants';
import './_style.scss';

const ExampleCredential = () => (
  <Col lg={12} xs={24} className="CredentialTemplate">
    <div className="CredentialHeader">
      <CellRenderer componentName="newCredential" title="degreeName" value={EXAMPLE_DEGREE_NAME} />
      <img
        className="IconUniversity"
        src="icon-free-university.svg"
        alt="Free University Tbilisi"
      />
    </div>
    <div className="CredentialContent">
      <CellRenderer
        componentName="newCredential"
        title="universityName"
        value={EXAMPLE_UNIVERSITY_NANE}
      />
      <hr />
      <CellRenderer componentName="newCredential" title="award" value={EXAMPLE_AWARD} />
      <hr />
      <CellRenderer componentName="newCredential" title="fullName" value={EXAMPLE_FULL_NAME} />
      <hr />
      <div className="DegreeDate">
        <CellRenderer
          componentName="newCredential"
          title="startDate"
          value={EXAMPLE_AWARD_START_DATE}
        />
        <CellRenderer
          componentName="newCredential"
          title="graduationDate"
          value={EXAMPLE_GRADUATION_DATE}
        />
      </div>
    </div>
  </Col>
);

export default ExampleCredential;

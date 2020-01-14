import React from 'react';
import PropTypes from 'prop-types';
import { Col } from 'antd';
import CellRenderer from '../CellRenderer/CellRenderer';
import freeUniLogo from '../../../../images/free-uni-logo.png';

import './_style.scss';

const CredentialSummaryData = ({ title, university, student, startDate, graduationDate, lg }) => (
  <Col lg={lg} xs={24} className="CredentialTemplate">
    <div className="CredentialHeader">
      <CellRenderer componentName="newCredential" title="degreeName" value={title} />
      <img className="IconUniversity" src={freeUniLogo} alt="Free University Tbilisi" />
    </div>
    <div className="CredentialContent">
      <CellRenderer componentName="newCredential" title="universityName" value={university} />
      <hr />
      {student && (
        <CellRenderer componentName="newCredential" title="fullName" value={student.fullname} />
      )}
      <hr />
      <div className="DegreeDate">
        {startDate && (
          <CellRenderer componentName="newCredential" title="startDate" value={startDate} />
        )}
        {graduationDate && (
          <CellRenderer
            componentName="newCredential"
            title="graduationDate"
            value={graduationDate}
          />
        )}
      </div>
    </div>
  </Col>
);

CredentialSummaryData.defaultProps = {
  lg: 12
};

CredentialSummaryData.propTypes = {
  title: PropTypes.string.isRequired,
  university: PropTypes.string.isRequired,
  student: PropTypes.string.isRequired,
  startDate: PropTypes.string.isRequired,
  graduationDate: PropTypes.string.isRequired,
  lg: PropTypes.number
};

export default CredentialSummaryData;

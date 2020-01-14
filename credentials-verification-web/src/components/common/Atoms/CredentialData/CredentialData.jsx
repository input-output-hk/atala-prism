import React from 'react';
import PropTypes from 'prop-types';
import { Col } from 'antd';
import CellRenderer from '../CellRenderer/CellRenderer';
import freeUniLgo from '../../../../images/FreeUniLogo.png';
import './_style.scss';

const CredentialData = ({ icon, title, university, award, student, startDate, graduationDate }) => (
  <Col lg={12} xs={24} className="CredentialTemplate">
    <div className="CredentialHeader">
      <CellRenderer componentName="newCredential" title="degreeName" value={title} />
      <img className="IconUniversity" src={icon || freeUniLgo} alt="Free University Tbilisi" />
    </div>
    <div className="CredentialContent">
      <CellRenderer componentName="newCredential" title="universityName" value={university} />
      <hr />
      <CellRenderer componentName="newCredential" title="award" value={award} />
      <hr />
      <CellRenderer componentName="newCredential" title="fullName" value={student} />
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

CredentialData.defaultProps = {
  award: '',
  icon: ''
};

CredentialData.propTypes = {
  icon: PropTypes.string,
  title: PropTypes.string.isRequired,
  university: PropTypes.string.isRequired,
  award: PropTypes.string,
  student: PropTypes.string.isRequired,
  startDate: PropTypes.string.isRequired,
  graduationDate: PropTypes.string.isRequired
};

export default CredentialData;

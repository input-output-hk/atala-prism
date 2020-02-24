import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import { Col } from 'antd';
import CellRenderer from '../CellRenderer/CellRenderer';
import freeUniLgo from '../../../../images/FreeUniLogo.png';
import './_style.scss';
import { LANDING_TITLE, LANDING_UNIVERSITY } from '../../../../helpers/constants';

const CredentialData = ({ icon, firstName, lastName, startDate, graduationDate }) => (
  <Col lg={12} xs={24} className="CredentialTemplate">
    <div className="CredentialHeader">
      <CellRenderer componentName="newCredential" title="degreeName" value={LANDING_TITLE} />
      <img className="IconUniversity" src={icon || freeUniLgo} alt="Free University Tbilisi" />
    </div>
    <div className="CredentialContent">
      <CellRenderer
        componentName="newCredential"
        title="universityName"
        value={LANDING_UNIVERSITY}
      />
      {firstName && (
        <Fragment>
          <hr />
          <CellRenderer
            componentName="newCredential"
            title="fullName"
            value={`${firstName} ${lastName}`}
          />
        </Fragment>
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

CredentialData.defaultProps = {
  icon: ''
};

CredentialData.propTypes = {
  icon: PropTypes.string,
  firstName: PropTypes.string.isRequired,
  lastName: PropTypes.string.isRequired,
  startDate: PropTypes.string.isRequired,
  graduationDate: PropTypes.string.isRequired
};

export default CredentialData;

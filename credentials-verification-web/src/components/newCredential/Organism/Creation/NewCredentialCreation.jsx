import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'antd';
import TemplateForm from '../TemplateForm/TemplateForm';
import ExampleCredential from '../../Atom/ExampleCredential/ExampleCredential';

import './_style.scss';

const NewCredentialCreation = ({ savePicture, formRef, credentialValues }) => (
  <Row type="flex" align="middle" className="NewCredentialCreation">
    <Col xs={24} lg={12} className="CredentialTemplateContainer">
      <ExampleCredential />
    </Col>
    <Col xs={24} lg={12} className="CredentialFormContainer">
      <TemplateForm savePicture={savePicture} credentialValues={credentialValues} ref={formRef} />
    </Col>
  </Row>
);

NewCredentialCreation.defaultProps = {
  credentialValues: {}
};

NewCredentialCreation.propTypes = {
  savePicture: PropTypes.func.isRequired,
  formRef: PropTypes.shape().isRequired,
  credentialValues: PropTypes.shape()
};

export default NewCredentialCreation;

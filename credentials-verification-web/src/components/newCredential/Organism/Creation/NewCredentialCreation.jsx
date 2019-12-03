import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'antd';
import TemplateForm from '../TemplateForm/TemplateForm';
import CredentialData from '../../../common/Atoms/CredentialData/CredentialData';
import {
  EXAMPLE_DEGREE_NAME,
  EXAMPLE_UNIVERSITY_NANE,
  EXAMPLE_AWARD,
  EXAMPLE_FULL_NAME,
  EXAMPLE_START_DATE,
  EXAMPLE_GRADUATION_DATE
} from '../../../../helpers/constants';

import './_style.scss';

const NewCredentialCreation = ({ savePicture, formRef, credentialValues }) => (
  <Row type="flex" align="middle" className="NewCredentialCreation">
    <Col xs={24} lg={12} className="CredentialTemplateContainer">
      <CredentialData
        title={EXAMPLE_DEGREE_NAME}
        university={EXAMPLE_UNIVERSITY_NANE}
        award={EXAMPLE_AWARD}
        student={EXAMPLE_FULL_NAME}
        startDate={EXAMPLE_START_DATE}
        graduationDate={EXAMPLE_GRADUATION_DATE}
      />
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

import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'antd';
import moment from 'moment';
import TemplateForm from '../TemplateForm/TemplateForm';
import CredentialData from '../../../common/Atoms/CredentialData/CredentialSummaryData';

import './_style.scss';
import { toProtoDate } from '../../../../APIs/__mocks__/helpers';

const NewCredentialCreation = ({
  savePicture,
  formRef,
  credentialValues,
  credentialData,
  updateExampleCredential
}) => {
  const { startDate, graduationDate } = credentialData;

  const formattedStartDate = startDate && toProtoDate(moment(startDate));
  const formattedGraduationDate = graduationDate && toProtoDate(moment(graduationDate));

  const formattedData = Object.assign({}, credentialData, {
    startDate: formattedStartDate,
    graduationDate: formattedGraduationDate
  });

  return (
    <Row type="flex" align="middle" className="NewCredentialCreation">
      <Col xs={24} lg={12} className="CredentialTemplateContainer">
        <CredentialData {...formattedData} />
      </Col>
      <Col xs={24} lg={12} className="CredentialFormContainer">
        <TemplateForm
          savePicture={savePicture}
          credentialValues={credentialValues}
          ref={formRef}
          updateExampleCredential={updateExampleCredential}
        />
      </Col>
    </Row>
  );
};

NewCredentialCreation.defaultProps = {
  credentialValues: {}
};

NewCredentialCreation.propTypes = {
  savePicture: PropTypes.func.isRequired,
  formRef: PropTypes.shape().isRequired,
  credentialValues: PropTypes.shape()
};

export default NewCredentialCreation;

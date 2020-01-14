import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'antd';
import moment from 'moment';
import TemplateForm from '../TemplateForm/TemplateForm';
import CredentialData from '../../../common/Atoms/CredentialData/CredentialSummaryData';
import { dayMonthYearFormatter } from '../../../../helpers/formatters';

import './_style.scss';

const NewCredentialCreation = ({
  savePicture,
  formRef,
  credentialValues,
  credentialData,
  updateExampleCredential
}) => {
  const { startDate, graduationDate } = credentialData;

  const formattedStartDate = startDate && dayMonthYearFormatter(moment(startDate));
  const formattedGraduationDate = graduationDate && dayMonthYearFormatter(moment(graduationDate));

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
  credentialValues: {},
  credentialData: {}
};

NewCredentialCreation.propTypes = {
  savePicture: PropTypes.func.isRequired,
  formRef: PropTypes.shape().isRequired,
  credentialValues: PropTypes.shape(),
  credentialData: PropTypes.shape(),
  updateExampleCredential: PropTypes.func.isRequired
};

export default NewCredentialCreation;

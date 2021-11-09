import React from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { Col, Row } from 'antd';
import moment from 'moment';
import TemplateForm from '../TemplateForm/TemplateForm';
import CredentialSummaryData from '../../../common/Atoms/CredentialData/CredentialSummaryData';
import { dayMonthYearFormatter } from '../../../../helpers/formatters';
import { getLogoAsBase64 } from '../../../../helpers/genericHelpers';
import { useSession } from '../../../../hooks/useSession';
import { refPropShape } from '../../../../helpers/propShapes';

import './_style.scss';

const NewCredentialCreation = observer(
  ({ savePicture, formRef, credentialValues, credentialData, updateExampleCredential }) => {
    const { startDate, graduationDate } = credentialData;

    const formattedStartDate = startDate && dayMonthYearFormatter(moment(startDate));
    const formattedGraduationDate = graduationDate && dayMonthYearFormatter(moment(graduationDate));

    const { session } = useSession();
    const userLogo = session.logo;
    const logo = getLogoAsBase64(userLogo);

    const formattedData = {
      ...credentialData,
      startDate: formattedStartDate,
      graduationDate: formattedGraduationDate,
      logo
    };

    return (
      <Row type="flex" align="middle" className="NewCredentialCreation">
        <Col xs={24} lg={12} className="CredentialTemplateContainer">
          <CredentialSummaryData {...formattedData} />
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
  }
);

NewCredentialCreation.defaultProps = {
  credentialValues: {},
  credentialData: {}
};

NewCredentialCreation.propTypes = {
  savePicture: PropTypes.func.isRequired,
  formRef: refPropShape.isRequired,
  credentialValues: PropTypes.shape(),
  credentialData: PropTypes.shape(),
  updateExampleCredential: PropTypes.func.isRequired
};

export default NewCredentialCreation;

import React from 'react';
import PropTypes from 'prop-types';
import { Col } from 'antd';
import CellRenderer from '../CellRenderer/CellRenderer';
import freeUniLogo from '../../../../images/free-uni-logo.png';

import './_style.scss';
import { EXAMPLE_AWARD } from '../../../../helpers/constants';
import { useTranslationWithPrefix } from '../../../../hooks/useTranslationWithPrefix';

const translationPrefix = 'newCredential.table.columns';

const CredentialSummaryData = ({
  title,
  university,
  student,
  startDate,
  graduationDate,
  lg,
  logo
}) => {
  const tp = useTranslationWithPrefix(translationPrefix);
  return (
    <Col lg={lg} xs={24} className="CredentialTemplate">
      <div className="CredentialHeader">
        <CellRenderer title={tp('universityName')} value={university} />
        <img className="IconUniversity" src={logo || freeUniLogo} alt="Free University Tbilisi" />
      </div>
      <div className="CredentialContent">
        <CellRenderer title={tp('degreeName')} value={title} />
        <hr />
        <CellRenderer title={tp('result')} value={EXAMPLE_AWARD} />
        <hr />
        {student && <CellRenderer title={tp('fullName')} value={student.fullname} />}
        <hr />
        <div className="DegreeDate">
          {startDate && <CellRenderer title={tp('startDate')} value={startDate} />}
          {graduationDate && <CellRenderer title={tp('graduationDate')} value={graduationDate} />}
        </div>
      </div>
    </Col>
  );
};

CredentialSummaryData.defaultProps = {
  lg: 12,
  logo: null,
  title: '',
  student: {
    fullname: ''
  },
  startDate: '',
  graduationDate: ''
};

CredentialSummaryData.propTypes = {
  title: PropTypes.string,
  university: PropTypes.string.isRequired,
  student: PropTypes.shape({ fullname: PropTypes.string }),
  startDate: PropTypes.string,
  graduationDate: PropTypes.string,
  lg: PropTypes.number,
  logo: PropTypes.string
};

export default CredentialSummaryData;

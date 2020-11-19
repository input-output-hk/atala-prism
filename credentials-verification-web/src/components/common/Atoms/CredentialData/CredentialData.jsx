import React from 'react';
import PropTypes from 'prop-types';
import { Col } from 'antd';
import CellRenderer from '../CellRenderer/CellRenderer';
import freeUniLgo from '../../../../images/FreeUniLogo.png';
import './_style.scss';
import { EXAMPLE_AWARD } from '../../../../helpers/constants';
import { useTranslationWithPrefix } from '../../../../hooks/useTranslationWithPrefix';

const translationKeyPrefix = 'newCredential.table.columns';

const CredentialData = ({ icon, title, university, award, student, startDate, graduationDate }) => {
  const tp = useTranslationWithPrefix(translationKeyPrefix);

  return (
    <Col lg={12} xs={24} className="CredentialTemplate">
      <div className="CredentialHeader">
        <CellRenderer title={tp('universityName')} value={university} />
        <img className="IconUniversity" src={icon || freeUniLgo} alt="Free University Tbilisi" />
      </div>
      <div className="CredentialContent">
        <CellRenderer title={tp('degreeName')} value={title} />
        <hr />
        <CellRenderer title={tp('result')} value={EXAMPLE_AWARD} />
        <hr />
        <CellRenderer title={tp('fullName')} value={student} />
        <hr />
        <div className="DegreeDate">
          {startDate && <CellRenderer title={tp('startDate')} value={startDate} />}
          {graduationDate && <CellRenderer title={tp('graduationDate')} value={graduationDate} />}
        </div>
      </div>
    </Col>
  );
};

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

import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { Row } from 'antd';
import PropTypes from 'prop-types';
import { shortDateFormatter } from '../../../../helpers/formatters';

import './_style.scss';

const DocumentAcceptation = ({ title, lastUpdated, content }) => {
  const { t } = useTranslation();

  return (
    <div className="AcceptTerms">
      <h2>{t(title)}</h2>
      <span>
        {t('registration.lastUpdated')}: {shortDateFormatter(lastUpdated)}
      </span>
      <div className="TermsContent">
        <p>{content}</p>
      </div>
    </div>
  );
};

DocumentAcceptation.propTypes = {
  title: PropTypes.string.isRequired,
  lastUpdated: PropTypes.number.isRequired,
  content: PropTypes.string.isRequired
};

export default DocumentAcceptation;

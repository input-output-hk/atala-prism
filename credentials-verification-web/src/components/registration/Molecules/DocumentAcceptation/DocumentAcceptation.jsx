import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import moment from 'moment';
import { shortDateFormatter } from '../../../../helpers/formatters';

import './_style.scss';

const DocumentAcceptation = ({ title, lastUpdated, content }) => {
  const { t } = useTranslation();

  return (
    <div className="AcceptTerms Wrapper">
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
  lastUpdated: PropTypes.instanceOf(moment).isRequired,
  content: PropTypes.string.isRequired
};

export default DocumentAcceptation;

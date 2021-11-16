import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';

import './_style.scss';

const QuotesPanel = () => {
  const { t } = useTranslation();

  return (
    <div className="QuotesPanelContent">
      <div className="QuotesPanel">
        <h1>{t('landing.quotes.phrase')}</h1>
      </div>
      <div className="triangle-down" />
    </div>
  );
};

export default QuotesPanel;

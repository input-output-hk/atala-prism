import React from 'react';
import { Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import './_style.scss';

const WhatYouNeed = ({ textNeed }) => {
  const { t } = useTranslation();
  return (
    <div className="WhatYouNeed">
      <h3>{t('landing.WhatYouNeed.Title')}</h3>
      <div className="WhatYouNeedContainer">
        <p>
          <Icon type="check" /> {textNeed}
        </p>
      </div>
    </div>
  );
};

WhatYouNeed.propTypes = {
  textNeed: PropTypes.string.isRequired
};

export default WhatYouNeed;

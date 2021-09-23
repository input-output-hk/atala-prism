import React from 'react';
import { Icon } from 'antd';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import PropTypes from 'prop-types';
import {
  GOVERNMENT_ISSUED_DIGITAL_IDENTITY,
  UNIVERSITY_DEGREE,
  PROOF_OF_EMPLOYMENT,
  INSURANCE_POLICY
} from '../../../../helpers/constants';
import './_style.scss';

const WhatYouNeed = ({ currentCredential }) => {
  const { t } = useTranslation();

  const scanCodeText = { text: t('landing.WhatYouNeed.scanCode') };
  const credentialIdText = {
    text: t('landing.WhatYouNeed.share'),
    strongText: t('credential.credentialNames.CredentialType0')
  };
  const credentialUniversityText = {
    text: t('landing.WhatYouNeed.share'),
    strongText: t('credential.credentialNames.CredentialType1')
  };
  const credentialEmploymentText = {
    text: t('landing.WhatYouNeed.share'),
    strongText: t('credential.credentialNames.CredentialType2')
  };

  const needListByCurrentCredential = {
    [GOVERNMENT_ISSUED_DIGITAL_IDENTITY]: [scanCodeText],
    [UNIVERSITY_DEGREE]: [scanCodeText, credentialIdText],
    [PROOF_OF_EMPLOYMENT]: [scanCodeText, credentialIdText, credentialUniversityText],
    [INSURANCE_POLICY]: [scanCodeText, credentialIdText, credentialEmploymentText]
  };

  const needItems = needListByCurrentCredential[currentCredential].map((item, index) => (
    <p key={index}>
      <Icon type="check" /> {item.text} <strong>{item.strongText}</strong>
    </p>
  ));
  return (
    <div className="WhatYouNeed">
      <div className="WhatYouNeedContainer">
        <h3>{t('landing.WhatYouNeed.Title')}</h3>
        {needItems}
      </div>
    </div>
  );
};

WhatYouNeed.propTypes = {
  currentCredential: PropTypes.number.isRequired
};

export default WhatYouNeed;

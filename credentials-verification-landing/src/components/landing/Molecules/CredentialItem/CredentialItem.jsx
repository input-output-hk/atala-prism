import React from 'react';
import { Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import LabelItem from '../../../common/Atoms/LabelItem/LabelItem';

import './_style.scss';
import { withRedirector } from '../../../providers/withRedirector';

const CredentialItem = ({ redirector: { redirectToCredential } }) => {
  const { t } = useTranslation();

  return (
    <div className="CredentialItem">
      <div className="CredentialImageContainer">
        <img src="images/credential-id.svg" alt={t('landing.findCredential.credentialAlt')} />
      </div>
      <div className="CredentialTextContainer">
        <LabelItem labelText="Credential ID" theme="Primary-Label" />
        <h2>{t('landing.findCredential.cardTitle')}</h2>
        <p>
          {t('landing.findCredential.cardSubtitle1')}
          {t('landing.findCredential.cardSubtitle2')}
        </p>
        <hr className="lineSeparator" />
        <p className="LittleText">{t('landing.findCredential.warning')}</p>
        <CustomButton
          buttonProps={{ className: 'theme-secondary', onClick: redirectToCredential }}
          buttonText={t('landing.findCredential.askForCredential')}
          icon={<Icon type="arrow-right" />}
        />
      </div>
    </div>
  );
};

CredentialItem.propTypes = {
  redirector: PropTypes.shape({ redirectToCredential: PropTypes.func }).isRequired
};

export default withRedirector(CredentialItem);

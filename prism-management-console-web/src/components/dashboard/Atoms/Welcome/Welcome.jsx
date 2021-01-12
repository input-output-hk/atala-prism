import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import welcomeImage from '../../../../images/welcome-img.png';
import { getThemeByRole } from '../../../../helpers/themeHelper';

import './_style.scss';
import { useSession } from '../../../providers/SessionContext';

const Welcome = ({ name, importantInfo }) => {
  const { t } = useTranslation();
  const { session } = useSession();
  const theme = getThemeByRole(session.userRole);

  return (
    <div className={`WelcomeContainer ${theme.class()}`}>
      <div className="WelcomeText">
        <h2 className={theme.class()}>{t('dashboard.welcome.title')}</h2>
        <p className={theme.class()}>
          {t('dashboard.welcome.paragraph')}
          <strong>{t('dashboard.welcome.atalaPrism')}</strong>
        </p>
      </div>
      <div className="WelcomeImg">
        <img src={welcomeImage} alt={t('dashboard.welcome.image')} />
      </div>
    </div>
  );
};

Welcome.defaultProps = {
  importantInfo: ''
};

Welcome.propTypes = {
  name: PropTypes.string.isRequired,
  importantInfo: PropTypes.string
};

export default Welcome;

import { Button } from 'antd';
import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import './_style.scss';

const UnderContsructionMessage = ({ goBack }) => {
  const { t } = useTranslation();

  return (
    <div className="UnderConstructionMessage">
      <h1>{t('generic.underConstruction')}</h1>
      <div className="ButtonContainer">
        <Button onClick={goBack}>{t('actions.back')}</Button>
      </div>
    </div>
  );
};

UnderContsructionMessage.propTypes = {
  goBack: PropTypes.func.isRequired
};

export default UnderContsructionMessage;

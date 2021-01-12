import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Checkbox } from 'antd';
import StepCard from '../../Atoms/StepCard/StepCard';
import PhraseRenderer from '../../Molecules/PhraseRenderer/PhraseRenderer';

import './_style.scss';

const SeedPhrase = ({ mnemonics, accepted, toggleAccept }) => {
  const { t } = useTranslation();

  return (
    <div className="RegisterStep">
      <StepCard
        title="registration.mnemonic.title"
        info="registration.mnemonic.explanation"
        subtitle="registration.mnemonic.request"
      />
      <div className="PhraseContainer">
        <PhraseRenderer mnemonics={mnemonics} />
      </div>
      <div className="CheckboxContainer">
        <Checkbox onChange={toggleAccept} checked={accepted} />
        <p>{t('registration.mnemonic.warning')}</p>
      </div>
    </div>
  );
};

SeedPhrase.propTypes = {
  mnemonics: PropTypes.arrayOf(PropTypes.shape()).isRequired,
  accepted: PropTypes.bool.isRequired,
  toggleAccept: PropTypes.func.isRequired
};

export default SeedPhrase;

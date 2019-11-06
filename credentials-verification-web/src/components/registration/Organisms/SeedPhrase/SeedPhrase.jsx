import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Checkbox, Row } from 'antd';
import StepCard from '../../Atoms/StepCard/StepCard';
import PhraseRenderer from '../../Molecules/PhraseRenderer/PhraseRenderer';

const SeedPhrase = ({ mnemonics, accepted, toggleAccept }) => {
  const { t } = useTranslation();

  return (
    <Fragment>
      <StepCard
        title="registration.mnemonic.title"
        info="registration.mnemonic.explanation"
        subtitle="registration.mnemonic.request"
      />
      <PhraseRenderer mnemonics={mnemonics} />
      <Row>
        <Checkbox onChange={toggleAccept} checked={accepted} />
        <label>{t('registration.mnemonic.warning')}</label>
      </Row>
    </Fragment>
  );
};

SeedPhrase.propTypes = {
  mnemonics: PropTypes.arrayOf(PropTypes.shape()).isRequired,
  accepted: PropTypes.bool.isRequired,
  toggleAccept: PropTypes.func.isRequired
};

export default SeedPhrase;

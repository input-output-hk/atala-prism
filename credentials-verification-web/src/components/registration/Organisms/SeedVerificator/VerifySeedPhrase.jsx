import React, { createRef, Fragment, useState } from 'react';
import PropTypes from 'prop-types';
import { Input, Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import StepCard from '../../Atoms/StepCard/StepCard';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';
import { noEmptyInput } from '../../../../helpers/formRules';
import VerifyFooter from '../../Atoms/VerifyFooter/VerifyFooter';

import './_style.scss';

const VerifySeedPhrase = ({ words, validMnemonic, setValidMnemonic }) => {
  const { t } = useTranslation();

  const [triedToVerify, setTriedToVerify] = useState(validMnemonic);

  const wordsRef = createRef();

  const validateWords = () => {
    const form = wordsRef.current.getForm();

    form.validateFieldsAndScroll((errors, { firstWord, secondWord }) => {
      if (!triedToVerify) setTriedToVerify(true);

      if (errors) return;

      const [{ value: originalFirstWord }, { value: originalSecondWord }] = words;
      const firstMatches = firstWord === originalFirstWord;
      const secondMatches = secondWord === originalSecondWord;

      const wordsMatch = firstMatches && secondMatches;
      if (!wordsMatch) {
        form.setFields({
          firstWord: {
            value: firstWord,
            errors: firstMatches
              ? null
              : [new Error(t('registration.mnemonic.validation.wordMissmatch'))]
          },
          secondWord: {
            value: secondWord,
            errors: secondMatches
              ? null
              : [new Error(t('registration.mnemonic.validation.wordMissmatch'))]
          }
        });
      }

      setValidMnemonic(wordsMatch);
    });
  };

  const createItem = ({ index: wordNumber }, index) => ({
    fieldDecoratorData: {
      rules: [noEmptyInput(t('errors.form.emptyField'))]
    },
    label: `${t('registration.mnemonic.word')} #${wordNumber + 1}`,
    key: `${index ? 'second' : 'first'}Word`,
    className: '',
    input: <Input />
  });

  const items = words.map((word, index) => createItem(word, index));

  return (
    <div className="RegisterStep">
      <StepCard
        title="registration.mnemonic.validation.title"
        subtitle="registration.mnemonic.validation.subtitle"
      />
      <CustomForm items={items} ref={wordsRef} />
      <VerifyFooter
        valid={validMnemonic}
        triedToVerify={triedToVerify}
        validateWords={validateWords}
      />
    </div>
  );
};

const mnemonicWordShape = PropTypes.shape({ index: PropTypes.number, value: PropTypes.string })
  .isRequired;

VerifySeedPhrase.propTypes = {
  words: PropTypes.arrayOf(mnemonicWordShape).isRequired,
  validMnemonic: PropTypes.bool.isRequired,
  setValidMnemonic: PropTypes.func.isRequired
};

export default VerifySeedPhrase;

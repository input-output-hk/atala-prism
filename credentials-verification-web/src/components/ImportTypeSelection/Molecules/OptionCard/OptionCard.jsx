import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Icon } from 'antd';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../../../helpers/constants';
import './_style.scss';

const OptionCard = ({ option, isSelected, onSelect, img, useCase }) => {
  const { t } = useTranslation();

  const optionCardClass = {
    [IMPORT_CONTACTS]: 'ImportContactOptionCard',
    [IMPORT_CREDENTIALS_DATA]: 'ImportCredentialsOptionCard'
  };

  const index = {
    bulkImport: 0,
    manualImport: 1
  };

  const handleKeyUp = () => {
    // TODO: define onKeyUp handler
    // what to do here?
  };

  return (
    <div
      className={`OptionCard ${optionCardClass[useCase]} ${
        isSelected ? 'Selected' : 'NotSelected'
      }`}
      onClick={() => onSelect(option)}
      onKeyUp={handleKeyUp}
      role="button"
      tabIndex={index[option]}
    >
      <div className="icon-container">
        <Icon
          className={isSelected ? 'icon' : 'icon-disabled'}
          type={isSelected ? 'check-circle' : 'check-circle'}
          theme="filled"
        />
      </div>
      <div className="img-container">
        <img src={img} alt="" />
      </div>
      <div className="CardText">
        <h1>{t(`${useCase}.${option}Card.title`)}</h1>
        <p>{t(`${useCase}.${option}Card.info`)}</p>
      </div>
    </div>
  );
};
OptionCard.defaultProps = {
  isSelected: false
};

OptionCard.propTypes = {
  option: PropTypes.string.isRequired,
  isSelected: PropTypes.bool,
  onSelect: PropTypes.func.isRequired,
  img: PropTypes.string.isRequired,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired
};

export default OptionCard;

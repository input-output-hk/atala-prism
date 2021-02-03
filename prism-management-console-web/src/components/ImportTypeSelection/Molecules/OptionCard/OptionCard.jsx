import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Tooltip } from 'antd';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../../../helpers/constants';
import './_style.scss';

const OptionCard = ({ option, isSelected, onSelect, img, useCase, disabled }) => {
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

  const handleClick = () => {
    if (!disabled) {
      onSelect(option);
    }
  };

  return (
    <Tooltip title={disabled ? t(`${useCase}.${option}Card.disabled`) : null}>
      <div
        className={`OptionCard ${optionCardClass[useCase]} ${
          isSelected ? 'Selected' : 'NotSelected'
        } ${disabled ? 'Disabled' : ''}`}
        onClick={handleClick}
        onKeyUp={handleKeyUp}
        role="button"
        tabIndex={disabled ? null : index[option]}
      >
        <div className="icon-container">
          <LegacyIcon
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
    </Tooltip>
  );
};

OptionCard.defaultProps = {
  isSelected: false,
  disabled: false
};

OptionCard.propTypes = {
  option: PropTypes.string.isRequired,
  isSelected: PropTypes.bool,
  onSelect: PropTypes.func.isRequired,
  img: PropTypes.string.isRequired,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  disabled: PropTypes.bool
};

export default OptionCard;

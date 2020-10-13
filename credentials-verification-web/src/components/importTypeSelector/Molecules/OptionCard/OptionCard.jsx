import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Icon } from 'antd';
import './_style.scss';

const OptionCard = ({ option, isSelected, onSelect, img }) => {
  const { t } = useTranslation();

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
      className={`OptionCard ${isSelected ? 'Selected' : 'NotSelected'}`}
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
        <h1>{t(`importContacts.${option}.title`)}</h1>
        <p>{t(`importContacts.${option}.info`)}</p>
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
  img: PropTypes.string.isRequired
};

export default OptionCard;

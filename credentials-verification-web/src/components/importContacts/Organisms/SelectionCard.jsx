import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Select, Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import './_style.scss';

const { Option } = Select;

const SelectionCard = ({ step, currentStep, title, description, options, onSelect }) => {
  const { t } = useTranslation();

  const [selection, setSelection] = useState();

  const handleSelect = value => {
    setSelection(value);
    onSelect(value.key);
  };

  const isDisabled = currentStep < step;

  return (
    <div className={`SelectionCard ${isDisabled ? 'DisabledCard' : null}`}>
      <div className="StepIcon">
        <h1>{step + 1}</h1>
      </div>
      <div className="CardText">
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
      <div className="SelectionWrapper">
        <Select
          labelInValue
          disabled={isDisabled}
          value={selection}
          placeholder="Select Type"
          onSelect={handleSelect}
          style={{ width: '75%' }}
          dropdownClassName="ContactsImportDropdown"
        >
          {options.map(o => (
            <Option className="OptionContainer" key={o.key}>
              <CustomOption icon={o.logo} label={t(o.label)} />
            </Option>
          ))}
        </Select>
      </div>
    </div>
  );
};

const CustomOption = ({ icon, label }) => (
  <>
    {icon && <Icon className="OptionIcon" component={icon} />}
    {label}
  </>
);

CustomOption.defaultProps = {
  icon: null
};

CustomOption.propTypes = {
  icon: PropTypes.elementType,
  label: PropTypes.string.isRequired
};

SelectionCard.defaultProps = {
  title: null,
  description: null,
  onSelect: () => {}
};

SelectionCard.propTypes = {
  step: PropTypes.number.isRequired,
  currentStep: PropTypes.number.isRequired,
  title: PropTypes.string,
  description: PropTypes.string,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired
    })
  ).isRequired,
  onSelect: PropTypes.func
};

export default SelectionCard;

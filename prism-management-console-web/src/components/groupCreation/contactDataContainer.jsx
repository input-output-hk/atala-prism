import React from 'react';
import './_style.scss';
import { Checkbox } from 'antd';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import img from '../../images/avatarIcon.svg';
import { dayMonthYearBackendFormatter } from '../../helpers/formatters';
import { dateObjectShape } from '../../helpers/propShapes';

const translatePrefix = 'contacts.table.columns';

const ContactDataContainer = ({
  contactName,
  creationDate,
  externalid,
  contactid,
  onSelectChange,
  selected
}) => {
  const { t } = useTranslation();

  function handleSelectChange(e) {
    onSelectChange({ checked: e.target.checked, contactid });
  }

  return (
    <div className="contactsDataGroups">
      <div className="groupsCheckbox">
        <Checkbox onChange={handleSelectChange} checked={selected} />
      </div>
      <img className="imgContactData" src={img} alt="" />
      <div className="textContactData">
        <h4>{t(`${translatePrefix}.contactName`)}</h4>
        <span>{contactName}</span>
      </div>
      <div className="textContactData">
        <h4>{t(`${translatePrefix}.externalid`)}</h4>
        <span>{externalid}</span>
      </div>
      <div className="textContactData">
        <h4>{t(`${translatePrefix}.creationDate`)}</h4>
        <span>{dayMonthYearBackendFormatter(creationDate)}</span>
      </div>
    </div>
  );
};

ContactDataContainer.propTypes = {
  contactName: PropTypes.string.isRequired,
  creationDate: PropTypes.shape(dateObjectShape).isRequired,
  externalid: PropTypes.string.isRequired,
  contactid: PropTypes.string.isRequired,
  onSelectChange: PropTypes.func.isRequired,
  selected: PropTypes.bool.isRequired
};

export default ContactDataContainer;

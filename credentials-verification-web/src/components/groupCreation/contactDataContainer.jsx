import React from 'react';
import './_style.scss';
import { Checkbox } from 'antd';
import img from '../../images/avatarIcon.svg';

const ContactDataContainer = ({ name, date, id }) => {
  function onChange(e) {
    console.log(`checked = ${e.target.checked}`);
  }
  return (
    <div className="contactsDataGroups">
      <div className="groupsCheckbox">
        <Checkbox onChange={onChange} />
      </div>
      <img className="imgContactData" src={img} alt="" />
      <div className="textContactData">
        <h4>Contact Name</h4>
        <span>{name}</span>
      </div>
      <div className="textContactData">
        <h4>External ID</h4>
        <span>{id}</span>
      </div>
      <div className="textContactData">
        <h4>Date Created</h4>
        <span>{date}</span>
      </div>
    </div>
  );
};

export default ContactDataContainer;

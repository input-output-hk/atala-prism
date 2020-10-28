import React from 'react';
import { useTranslation } from 'react-i18next';
import './_style.scss';
import IdIcon from '../../../../images/IdIcon.svg';
import peopleIcon from '../../../../images/iconpeople.svg';

const RowInfo = ({ theme, groupName, contacts }) => {
  const { t } = useTranslation();
  const classname = `RowInfo ${theme}`;
  return (
    <div className={classname}>
      <div className="groupNameContainer">
        <div className="img">
          <img className="IconUniversity" src={IdIcon} alt="Icon University" />
        </div>
        <div className="groupTitle">
          <p>{t('contacts.detail.dataTitle')}</p>
          <span>{groupName}</span>
        </div>
      </div>
      <div className="contactsTitle">
        <div>
          <img className="img" src={peopleIcon} alt="Icon Group" />
        </div>
        <div className="contactsContainer">
          <p>{contacts}</p>
          <span style={{ textTransform: 'uppercase' }}>{t('contacts.detail.contacts')}</span>
        </div>
      </div>
    </div>
  );
};

export default RowInfo;

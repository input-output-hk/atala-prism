import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import './_style.scss';
import IdIcon from '../../../../images/IdIcon.svg';
import peopleIcon from '../../../../images/iconpeople.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const Row = ({ theme, groupName, contacts, groupId, contactId, onDelete }) => {
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
      {onDelete && (
        <CustomButton
          buttonProps={{
            onClick: () => onDelete(groupId, [contactId]),
            className: 'theme-link'
          }}
          buttonText={t('actions.delete')}
        />
      )}
    </div>
  );
};

Row.defaultProps = {
  theme: 'row-info',
  onDelete: null,
  contactId: ''
};

Row.propTypes = {
  theme: PropTypes.string,
  groupName: PropTypes.string.isRequired,
  contacts: PropTypes.number.isRequired,
  groupId: PropTypes.string.isRequired,
  contactId: PropTypes.string,
  onDelete: PropTypes.func
};

export default Row;

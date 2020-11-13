import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';
import Contact from './Contact';
import Loading from '../common/Atoms/Loading/Loading';

const ContactContainer = ({ api }) => {
  const { t } = useTranslation();
  const { id } = useParams();

  const [contact, setContact] = useState();
  const [groups, setGroups] = useState();

  const getContact = () =>
    api.contactsManager
      .getContact(id)
      .then(({ jsondata, ...rest }) => {
        const contactData = Object.assign(JSON.parse(jsondata), rest);
        setContact(contactData);
      })
      .catch(error => {
        Logger.error(`[ContactContainer.getContact] Error while getting contact ${id}`, error);
        message.error(t('errors.errorGetting', { model: 'contact' }));
      });

  const getGroups = () =>
    api.groupsManager
      .getGroups(id)
      .then(setGroups)
      .catch(error => {
        Logger.error(
          `[ContactContainer.getGroups] Error while getting groups for contact ${id}`,
          error
        );
        message.error(t('errors.errorGetting', { model: 'groups' }));
      });

  useEffect(() => {
    getContact();
    getGroups();
  }, []);

  return !contact && !groups ? <Loading /> : <Contact contact={contact} groups={groups} />;
};

ContactContainer.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({ getContact: PropTypes.func }),
    groupsManager: PropTypes.shape({ getGroups: PropTypes.func })
  }).isRequired
};

export default withApi(ContactContainer);

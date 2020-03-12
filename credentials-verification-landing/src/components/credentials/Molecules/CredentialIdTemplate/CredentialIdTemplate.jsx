import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { useTranslation } from 'react-i18next';
import { Col, Row } from 'antd';
import flagIcon from '../../../../images/icon-flag.svg';
import avatarId from '../../../../images/id_generic_avatar.svg';
import { CARD_ID_ID_NUMBER, CARD_ID_EXPIRATION_DATE } from '../../../../helpers/constants';

import './_style.scss';

const CredentialIDTemplate = ({ firstName, lastName, dateOfBirth }) => {
  const { t } = useTranslation();

  return (
    <div className="CredentialIDTemplate">
      <div className="HeaderTemplate">
        <div className="Headertitle">
          <span>{t('credential.IdCredentialCard.CredentialTitle')}</span>
          <p>{t('credential.IdCredentialCard.CredentialSubtitle')}</p>
        </div>
        <img className="FlagIcon" src={flagIcon} alt="Flag Icon" />
      </div>
      <div className="ContentTemplate">
        <Row>
          <Col xs={24} lg={6}>
            <img className="AvatarId" src={avatarId} alt="Avatar Icon" />
          </Col>
          <Col xs={24} lg={18} className="InfoTemplate">
            <div className="TemplateItem">
              <span>{t('credential.IdCredentialCard.IdentityNumber')}</span>
              <p>{CARD_ID_ID_NUMBER}</p>
            </div>
            <div className="TemplateItem">
              <span>{t('credential.IdCredentialCard.DateOfBirth')}</span>
              <p>{moment(dateOfBirth).format('DD/MM/YYYY')}</p>
            </div>
            <div className="TemplateItem">
              <span>{t('credential.IdCredentialCard.FullName')}</span>
              <p>{`${firstName} ${lastName}`}</p>
            </div>
            <div className="TemplateItem">
              <span>{t('credential.IdCredentialCard.ExpirationDate')}</span>
              <p>{moment.unix(CARD_ID_EXPIRATION_DATE).format('DD/MM/YYYY')}</p>
            </div>
          </Col>
        </Row>
      </div>
    </div>
  );
};

CredentialIDTemplate.propTypes = {
  firstName: PropTypes.string.isRequired,
  lastName: PropTypes.string.isRequired,
  dateOfBirth: PropTypes.string.isRequired
};

export default CredentialIDTemplate;

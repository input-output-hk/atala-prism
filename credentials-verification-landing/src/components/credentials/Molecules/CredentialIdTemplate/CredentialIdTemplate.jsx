import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { useTranslation } from 'react-i18next';
import { Col, Row } from 'antd';
import md5Hex from 'md5-hex';
import flagIcon from '../../../../images/icon-flag.svg';
import avatarId from '../../../../images/id_generic_avatar.svg';

import './_style.scss';

const CredentialIDTemplate = ({ firstName, dateOfBirth }) => {
  const { t } = useTranslation();
  const uniqueData = firstName + moment(dateOfBirth).format('YYYY-MM-DD');
  const identityNumber = md5Hex(uniqueData)
    .substring(0, 9)
    .toUpperCase();

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
              <p>{`RL-${identityNumber}`}</p>
            </div>
            <div className="TemplateItem">
              <span>{t('credential.IdCredentialCard.DateOfBirth')}</span>
              <p>{moment(dateOfBirth).format('YYYY-MM-DD')}</p>
            </div>
            <div className="TemplateItem">
              <span>{t('credential.IdCredentialCard.FullName')}</span>
              <p>{firstName}</p>
            </div>
            <div className="TemplateItem">
              <span>{t('credential.IdCredentialCard.ExpirationDate')}</span>
              <p>
                {moment()
                  .add(10, 'y')
                  .format('YYYY-MM-DD')}
              </p>
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

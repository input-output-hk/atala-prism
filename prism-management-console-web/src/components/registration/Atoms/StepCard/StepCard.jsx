import React from 'react';
import { useTranslation } from 'react-i18next';
import { Col, Row } from 'antd';
import PropTypes from 'prop-types';
import icon from '../../../../images/registrationMan.svg';

import './_style.scss';

const StepCard = ({ comment, title, info, subtitle }) => {
  const { t } = useTranslation();

  return (
    <div className="StepCard">
      <Row>
        <Col span={14}>
          {/* eslint-disable-next-line */}
          <span>{t(comment)}</span>
          {/* eslint-disable-next-line */}
          <h1>{t(title)}</h1>
          {/* eslint-disable-next-line */}
          <p>{t(info)}</p>
          {/* eslint-disable-next-line */}
          <p>{t(subtitle)}</p>
        </Col>
        <Col span={8} className="RegistrationImg">
          <img src={icon} alt={t('registration.alt')} />
        </Col>
      </Row>
    </div>
  );
};

StepCard.defaultProps = {
  comment: '',
  info: '',
  subtitle: ''
};

StepCard.propTypes = {
  comment: PropTypes.string,
  title: PropTypes.string.isRequired,
  info: PropTypes.string,
  subtitle: PropTypes.string
};

export default StepCard;

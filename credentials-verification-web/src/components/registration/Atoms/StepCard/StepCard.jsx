import React from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Col } from 'antd';
import PropTypes from 'prop-types';
import icon from '../../../../images/registrationMan.svg';

const StepCard = ({ comment, title, info, subtitle }) => {
  const { t } = useTranslation();

  return (
    <Card>
      <Col>
        {/* eslint-disable-next-line */}
        <label>{t(comment)}</label>
        {/* eslint-disable-next-line */}
        <h1>{t(title)}</h1>
        {/* eslint-disable-next-line */}
        <label>{t(info)}</label>
        {/* eslint-disable-next-line */}
        <label>{t(subtitle)}</label>
      </Col>
      <Col>
        <img src={icon} alt={t('registration.alt')} />
      </Col>
    </Card>
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

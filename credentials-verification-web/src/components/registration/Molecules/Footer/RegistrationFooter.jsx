import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox, Col, Row } from 'antd';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const RegistrationFooter = ({ next, previous, accepted, toggleAccept, documentToAccept }) => {
  const { t } = useTranslation();

  return (
    <Row>
      {previous && (
        <Col>
          <CustomButton
            buttonProps={{
              onClick: previous,
              className: 'theme-grey'
            }}
            buttonText={t('registration.back')}
          />
        </Col>
      )}
      {toggleAccept && (
        <Col>
          <Checkbox onChange={toggleAccept} checked={accepted} /> {t('registration.accept')}
          {t(`registration.${documentToAccept}`)}
        </Col>
      )}
      <Col>
        <Link to="/">{t('registration.cancel')}</Link>
      </Col>
      <Col>
        <CustomButton
          buttonProps={{
            onClick: next,
            className: 'theme-primary'
          }}
          buttonText={t('registration.next')}
        />
      </Col>
    </Row>
  );
};

RegistrationFooter.defaultProps = {
  next: null,
  previous: null,
  toggleAccept: null,
  documentToAccept: ''
};

RegistrationFooter.propTypes = {
  next: PropTypes.func,
  previous: PropTypes.func,
  accepted: PropTypes.bool.isRequired,
  toggleAccept: PropTypes.func,
  documentToAccept: PropTypes.string
};

export default RegistrationFooter;

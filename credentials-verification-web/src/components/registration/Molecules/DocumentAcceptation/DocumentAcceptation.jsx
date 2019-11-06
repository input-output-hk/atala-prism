import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { Row } from 'antd';
import PropTypes from 'prop-types';
import { shortDateFormatter } from '../../../../helpers/formatters';

const DocumentAcceptation = ({ title, lastUpdated, content }) => {
  const { t } = useTranslation();

  return (
    <Fragment>
      <Row>{t(title)}</Row>
      <Row>
        {t('registration.lastUpdated')}: {shortDateFormatter(lastUpdated)}
      </Row>
      <Row>{content}</Row>
    </Fragment>
  );
};

DocumentAcceptation.propTypes = {
  title: PropTypes.string.isRequired,
  lastUpdated: PropTypes.number.isRequired,
  content: PropTypes.string.isRequired
};

export default DocumentAcceptation;

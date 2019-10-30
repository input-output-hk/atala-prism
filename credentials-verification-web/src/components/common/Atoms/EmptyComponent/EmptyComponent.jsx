import React from 'react';
import { Col, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import './_style.scss';

const EmptyComponent = ({ photoSrc, photoAlt, title, subtitle, button }) => {
  const { t } = useTranslation();

  const rowProps = { align: 'middle', gutter: '32', justify: 'center', type: 'flex' };

  return (
    <div className="EmptyState">
      {photoSrc && <img src={photoSrc} alt={t(photoAlt)} />}
      {title && <h3>{t(title)}</h3>}
      {subtitle && <p>{t(subtitle)}</p>} {button && <div className="ActionButton">{button}</div>}
    </div>
  );
};

EmptyComponent.defaultProps = {
  photoSrc: '',
  photoAlt: '',
  title: '',
  subtitle: '',
  button: null
};

EmptyComponent.propTypes = {
  photoSrc: PropTypes.string,
  photoAlt: PropTypes.string,
  title: PropTypes.string,
  subtitle: PropTypes.string,
  button: PropTypes.element
};

export default EmptyComponent;

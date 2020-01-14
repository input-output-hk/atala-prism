import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import './_style.scss';

const EmptyComponent = ({ photoSrc, button, model, isFilter }) => {
  const { t } = useTranslation();
  const title = isFilter ? 'filterTitle' : 'title';

  return (
    <div className="EmptyState">
      {photoSrc && <img src={photoSrc} alt={t('emptyComponent.photoAlt', { model })} />}
      {<h3>{t(`emptyComponent.${title}`, { model })}</h3>}
      {button && <p>{t(`emptyComponent.${button ? 'subtitleWithGroup' : 'subtitle'}`)}</p>}
      {button && <div className="ActionButton">{button}</div>}
    </div>
  );
};

EmptyComponent.defaultProps = {
  photoSrc: '',
  isFilter: false,
  button: null
};

EmptyComponent.propTypes = {
  photoSrc: PropTypes.string,
  isFilter: PropTypes.bool,
  button: PropTypes.element,
  model: PropTypes.oneOf(['connection']).isRequired
};

export default EmptyComponent;

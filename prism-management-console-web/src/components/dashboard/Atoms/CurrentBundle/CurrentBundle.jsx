import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import valueSeparator from '../../../../ValueSeparator.svg';

import './_style.scss';

const InverseCell = ({ value, title }) => (
  <div className="BundleValues">
    <h1>{value}</h1>
    <p>{title}</p>
  </div>
);

InverseCell.propTypes = {
  value: PropTypes.number.isRequired,
  title: PropTypes.string.isRequired
};

const CurrentBundle = ({ bundle }) => {
  const { t } = useTranslation();

  // eslint-disable-next-line jsx-a11y/label-has-for
  if (!bundle) return <label>{t('dashboard.bundle.noBundle')}</label>;
  const { remaining, total } = bundle;

  return (
    <div className="CurrentBundleContainer">
      <h3>{t('dashboard.bundle.title')}</h3>
      <div className="BundleData">
        <InverseCell value={remaining} title={t('dashboard.bundle.remaining')} />
        <img src={valueSeparator} alt={t('dashboard.welcome.image')} />
        <InverseCell value={total} title={t('dashboard.bundle.total')} />
      </div>
    </div>
  );
};

CurrentBundle.defaultProps = {
  bundle: { remaining: 10, total: 100 }
};

CurrentBundle.propTypes = {
  bundle: PropTypes.shape({
    remaining: PropTypes.number,
    total: PropTypes.number
  })
};

export default CurrentBundle;
